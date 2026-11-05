# LinkDot · 用户匹配系统

> 一个「按标签找搭子」的前后端分离全栈应用：Spring Boot 后端 + Vue 3 前端，
> 以 **Docker Compose 编排、环境变量注入、脚本化交付** 的方式完整跑在单台 2C2G 云服务器上。
>
> 项目定位：**可交付、可运维、可复现**的小型生产系统 —— 不只是「本机能跑」，而是「换台机器照着文档 20 分钟能上线」。

---

## 一、项目简介

LinkDot 面向兴趣社群场景，解决「找同好、找队友」的问题。用户注册后维护自己的标签（技术栈、运动、爱好等），
系统据此做两件事：

- **推荐**：首页推荐最新活跃用户，推荐结果进入 Redis 缓存，由定时任务提前预热；
- **匹配（心动模式）**：基于标签集合的**编辑距离**计算相似度，用**倒排预筛 + 精算**两段式查询，5 万级用户数据下仍保持毫秒级响应。

在此之上提供**队伍**能力（创建、检索、加入、退出、成员管理），形成完整的「发现人 → 组队 → 协作」闭环。

项目同时是一份**运维实施练习**：四容器编排、健康检查、日志轮转、数据持久化、密钥外置、一键部署脚本、
故障排查记录全部沉淀在仓库里，`README` 即可当作运维手册使用。

---

## 二、技术栈

| 层次 | 选型 | 说明 |
| --- | --- | --- |
| 后端框架 | Spring Boot 2.6.4（Java 8） | Web MVC、拦截器、统一异常处理 |
| 持久层 | MyBatis-Plus 3.5.1 + MySQL 8.0 | 逻辑删除、分页插件、XML 复杂查询 |
| 缓存 / 会话 | Redis 7 + Spring Session | 分布式 Session、热点数据缓存、Redisson 分布式锁 |
| 文件存储 | 阿里云 OSS（SDK 3.17.4） | 头像上传，凭证从环境变量读取 |
| 接口文档 | Knife4j 4.4.0（OpenAPI 2） | 在线调试，路径 `/api/doc.html` |
| 前端 | Vue 3.5 + TypeScript + Vite 8 | Composition API、`<script setup>`、Vant 4 组件库 |
| 工程化 | ESLint + Prettier + vue-tsc | 提交前静态检查，`build` 阶段做类型校验 |
| 部署 | Docker / Docker Compose / Nginx | 四容器编排，Nginx 托管静态资源并反代 `/api` |
| 运维脚本 | Python 3（Paramiko）/ Shell / Batch | 一键构建发布、启停、健康核对 |

**运行环境要求**：JDK 8+（构建可用 17）、Maven 3.6+、Node.js 20+、MySQL 8.0、Redis 7、Docker 24+ / Compose v2。

---

## 三、系统架构

```
                         ┌──────────────────────── 浏览器 ────────────────────────┐
                         │                                                        │
                         ▼                                                        │
              ┌──────────────────────┐                                          │
   80/tcp ───▶│  nginx (frontend)    │                                          │
              │  · /     → Vue dist  │                                          │
              │  · /api/ → 反向代理  │                                          │
              └──────────┬───────────┘                                          │
                         │ 容器内部网络（不对外暴露端口）                          │
                         ▼                                                      │
              ┌──────────────────────┐        ┌──────────────────────┐          │
              │  Spring Boot 后端     │───────▶│  MySQL 8.0           │          │
              │  context-path: /api   │        │  user_match_db       │          │
              │  JVM 512m 上限        │        └──────────────────────┘          │
              └──────────┬───────────┘                                          │
                         │                                                      │
                         ├───────────────▶ Redis 7（Session / 缓存 / 分布式锁）  │
                         └───────────────▶ 阿里云 OSS（头像）                    │
```

**关键设计**

- **单入口**：只有 Nginx 暴露 80 端口，MySQL / Redis / 后端全部在内部网络，缩小攻击面；
- **前后端同源**：前端生产环境 `VITE_API_BASE_URL=/api`，由 Nginx 反代，天然规避跨域与 Cookie 问题；
- **无状态后端**：Session 存 Redis，同一份镜像可水平扩容（定时任务已用分布式锁防止重复执行）。

---

## 四、功能说明

### 4.1 用户与账号

| 功能 | 说明 |
| --- | --- |
| 注册 / 登录 / 注销 | MD5 + 盐值存储密码；登录态写入 Redis Session，有效期 24h |
| 个人信息管理 | 昵称、性别、电话、邮箱、简介、头像；返回统一走 `getSafetyUser` 脱敏，密码不出接口 |
| 头像上传 | 上传至 OSS，文件名 UUID 化避免覆盖；未设置时前端使用默认头像兜底 |
| 标签维护 | 标签新增 / 修改 / 删除，双写 `user.tags`（JSON）与 `user_tag` 关联表（供检索走索引） |
| 权限控制 | `userRole` 区分普通用户与管理员，管理端接口做二次校验 |

### 4.2 推荐与匹配

- **首页推荐**：按注册时间倒序分页；结果写 Redis（30s TTL），`PreCacheJob` 定时预热默认页，冷启动不打穿数据库；
- **心动模式**：
  1. 校验当前用户是否有标签，无标签则引导补充（前端拦截 + 后端兜底）；
  2. 倒排预筛：只取「至少一个共同标签」的候选集，按共同标签数排序取 Top N，避免全表扫描；
  3. 精算：对候选集计算标签集合的**编辑距离**，升序取最相似的用户；
  4. 结果缓存 30 分钟，标签变更时主动失效对应缓存。

### 4.3 队伍

| 功能 | 说明 |
| --- | --- |
| 创建队伍 | 设置名称、描述、最大人数、过期时间、入队状态（公开 / 私有 / 加密） |
| 队伍检索 | 支持按名称模糊、状态、人数等条件分页查询，展示队长与当前人数 |
| 加入 / 退出 | 校验人数上限、重复加入、私有队伍需密码；队长退出时做转移或解散处理 |
| 我的队伍 | 「我创建的」「我加入的」两个列表，成员维度的操作入口 |

### 4.4 工程与运维能力

- 统一响应体 `BaseResponse` + 错误码枚举 + `GlobalExceptionHandler`，前端只需处理一套结构；
- Knife4j 在线接口文档，便于联调与交付验收；
- 全量配置外置：数据库 / Redis / OSS / 跨域白名单均由环境变量注入；
- 逻辑删除（`isDelete`）保留数据可追溯，避免物理删除带来的误操作风险。

---

## 五、部署与运行

### 5.1 方式一：本地开发（前后端分离）

```bash
# 1) 准备中间件：MySQL 8.0（库 user_match_db）+ Redis 7
#    建表：mysql -uroot -p user_match_db < deploy/mysql/init/schema.sql

# 2) 启动后端（默认读取 application.yml，连接 localhost:3306 / 127.0.0.1:6379）
cd user_match_backend
./mvnw spring-boot:run          # 或 mvn spring-boot:run
# 接口地址：http://localhost:8080/api    文档：http://localhost:8080/api/doc.html

# 3) 启动前端（Vite 开发服务器 5173，.env.development 指向 8080，后端已放行该来源跨域）
cd user_match_frontend
npm ci
npm run dev                     # http://localhost:5173
```

Windows 下可直接双击 `scripts/start-backend.bat` / `scripts/start-frontend.bat`，停止用对应的 `stop-*.bat`（按端口精确查 PID，不会误杀其他进程）。

### 5.2 方式二：Docker Compose 一键部署（推荐）

```bash
# 1) 构建产物（后端可执行 JAR + 前端静态资源）
cd user_match_backend  && ./mvnw -DskipTests package && cd ..
cd user_match_frontend && npm ci && npm run build && cd ..

# 2) 准备环境变量（密钥不入库，模板已给出全部必填项）
cd deploy
cp .env.example .env
vi .env          # 填写 MYSQL_PASSWORD / REDIS_PASSWORD / OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET

# 3) 编排启动（首次会自动建库建表）
docker compose up -d --build
docker compose ps            # 4 个容器 Up，mysql/redis 显示 (healthy)
```

启动顺序由 `depends_on` + `healthcheck` 保证：**MySQL / Redis 健康检查通过 → 后端启动 → Nginx 启动**，
避免「后端先起来连不上库，容器反复重启」这一常见事故。

部署后自检：

```bash
curl -I  http://127.0.0.1/              # 前端 200
curl -s  http://127.0.0.1/api/user/tags # {"code":0,"data":[...],"message":"ok"}
docker logs user-match-backend | tail   # 出现 "Started UserCenterApplication" 即启动成功
```

### 5.3 自动化发布：一条命令完成上线

`scripts/deploy.py` 把「构建 → 增量上传 → 远端重建 → 状态核对」串成幂等流水线，服务器目录结构与仓库保持一致，
杜绝「两份部署文件、改一处漏一处」：

```bash
pip install paramiko

# 密钥登录（推荐）
DEPLOY_HOST=<服务器地址> DEPLOY_KEY=~/.ssh/id_rsa python scripts/deploy.py

# 密码登录（运行时交互输入，不落盘、不入库）
DEPLOY_HOST=<服务器地址> python scripts/deploy.py
```

脚本行为：`npm ci && npm run build` → `mvn -B -DskipTests package` → SFTP 上传 JAR / dist / 编排文件 →
`docker compose up -d --build` → 15s 后打印 `docker compose ps` → 清理悬空镜像。
任何一步失败立即中断并退出（`set -e` 语义），不会出现「半套代码已上线」的中间态。

### 5.4 日常运维

| 场景 | 命令（在 `deploy/` 目录执行） |
| --- | --- |
| 查看状态 | `docker compose ps` |
| 实时日志 | `docker compose logs -f --tail 200 backend` |
| 单服务重启 | `docker compose restart backend` |
| 仅后端更新 | 重新 `mvn package` 后 `docker compose up -d --build backend` |
| 仅前端更新 | 重新 `npm run build`（dist 为只读挂载，无需重启容器） |
| 全量重建 | `docker compose down && docker compose up -d` |
| 备份数据库 | `docker exec user-match-mysql mysqldump -uroot -p"$MYSQL_PASSWORD" user_match_db > backup.sql` |

### 5.5 故障排查手册（真实踩坑记录）

| 现象 | 根因 | 处置 |
| --- | --- | --- |
| Redis 容器一直 `unhealthy`，后端随之无法启动 | Compose 未给 redis 容器声明 `REDIS_PASSWORD`，健康检查命令取到空值 | 在 `environment` 中补齐变量，`up -d` 重建 |
| 后端启动即退出，日志报 `Access denied` | 数据库密码含特殊字符 / 未注入环境变量 | 统一由 `deploy/.env` 注入，禁止硬编码 |
| 镜像构建报 `COPY failed` | Dockerfile 中的 JAR 名与 `pom.xml` 的 `artifactId/version` 不一致 | 产物名与 `pom.xml` 对齐（本项目已修复历史遗留的模板文件名） |
| 前端页面 200 但接口 404 | Nginx 反代丢失 `/api` 前缀 | `proxy_pass http://backend:8080/api/;` 保留前缀，context-path 一致 |
| 2C2G 机器 OOM | 默认 JVM 堆过大 | 镜像内 `-Xms256m -Xmx512m` 固定堆上限，并限制容器日志 `max-size=20m / max-file=3` |
| 时间比本地差 8 小时 | 容器时区为 UTC | 全容器设置 `TZ=Asia/Shanghai`，JVM 追加 `-Duser.timezone` |

### 5.6 压测数据（可选）

`tools/gen_fake_users.py` 可批量生成仿真用户与标签关系（默认 5 万用户 / 27 万条标签关联），
用于验证分页、推荐与匹配性能，导入后可直接观察缓存命中与响应耗时：

```bash
python tools/gen_fake_users.py                 # 生成 SQL 文件
docker exec -i user-match-mysql sh -c \
  "mysql --default-character-set=utf8mb4 -uroot -p\$MYSQL_PASSWORD user_match_db" < fake_50k_users.sql
```

---

## 六、目录结构

```
user_match_system/
├── user_match_backend/                 # 后端（Spring Boot）
│   ├── src/main/java/com/howie/usermatch/
│   │   ├── controller/                 # UserController / TeamController
│   │   ├── service/ + service/impl/     # 业务逻辑（匹配算法、缓存、事务校验）
│   │   ├── mapper/                     # MyBatis-Plus Mapper 接口
│   │   ├── model/                      # domain / vo / dto / request / enums 分层
│   │   ├── config/                     # CORS、MyBatis-Plus、Redis、Redisson、Knife4j
│   │   ├── job/PreCacheJob.java        # 缓存预热定时任务（分布式锁防重复执行）
│   │   ├── common/                     # 统一响应、错误码、分页请求
│   │   ├── exception/                  # 业务异常 + 全局异常处理
│   │   └── utils/                      # 编辑距离算法、OSS 上传工具
│   ├── src/main/resources/
│   │   ├── application.yml             # 公共配置（全部为环境变量占位符）
│   │   ├── application-prod.yml        # 生产 profile
│   │   └── mapper/*.xml                # 复杂 SQL（候选集预筛等）
│   ├── .mvn/ + mvnw + mvnw.cmd         # Maven Wrapper，克隆即可构建
│   └── pom.xml
├── user_match_frontend/                # 前端（Vue 3 + TS + Vite）
│   ├── src/pages/                      # 13 个页面（首页 / 搜索 / 队伍 / 个人中心…）
│   ├── src/components/                 # UserCardList / TeamCardList
│   ├── src/plugins/myAxios.ts          # axios 封装：环境化 baseURL、统一错误处理
│   ├── src/models|services|states/     # 类型定义 / 接口调用 / 登录态
│   ├── src/config/route.ts             # 路由表
│   ├── .env.development / .env.production
│   └── vite.config.ts
├── deploy/                             # 部署编排（与代码同仓，随版本一起演进）
│   ├── docker-compose.yml              # 四容器编排 + 健康检查 + 日志轮转
│   ├── backend/Dockerfile              # 运行时镜像（仅 JRE + JAR）
│   ├── nginx/nginx.conf                # 静态托管 + /api 反代
│   ├── mysql/init/schema.sql           # 首次启动自动建表
│   └── .env.example                    # 环境变量模板（真实 .env 已被忽略）
├── scripts/
│   ├── deploy.py                       # 一键构建发布（Paramiko + Compose）
│   └── start/stop-*.bat                # Windows 本地启停
├── tools/gen_fake_users.py             # 压测数据生成
├── .dockerignore / .gitignore
└── README.md
```

---

## 七、个人亮点

### 7.1 运维实施（项目重点）

1. **容器化交付，一次编排到位**
   四容器（Nginx / 后端 / MySQL / Redis）由单份 `docker-compose.yml` 描述，`depends_on` + `healthcheck`
   严格控制启动顺序；数据用命名卷持久化（MySQL 数据、Redis AOF），重建容器不丢数据；`restart: unless-stopped`
   保证进程异常或宿主机重启后自愈。

2. **配置与代码彻底分离**
   数据库、Redis、OSS 凭证、跨域白名单全部走环境变量；本地开发用 `application.yml` 默认值，生产用 `application-prod.yml`
   覆盖；仓库只提交 `.env.example`，真实 `.env` 被 `.gitignore` 拦截 —— 换环境只改一份变量文件，不动一行代码。

3. **交付过程脚本化、可复现**
   `scripts/deploy.py` 将构建、上传、远端重建、状态核对串成幂等流水线，任一步失败即中断；
   服务器目录结构与仓库一致，避免部署文件与源码「双份维护」漂移。本地另有按端口精确停服的 Windows 启停脚本。

4. **面向小规格服务器的稳定性调优**
   2C2G 机器上固定 JVM 堆（`-Xms256m -Xmx512m`）防止 OOM；容器日志 `max-size=20m / max-file=3` 防止磁盘被打满；
   统一时区避免时间错位；仅暴露 80 端口，其余服务留在内部网络。

5. **缓存与并发做了防踩踏设计**
   Redis Session 让后端无状态、可水平扩展；推荐数据定时预热 + 短 TTL，冷启动不打穿数据库；
   定时任务用 Redisson 分布式锁，多实例部署时只有一台真正执行。

6. **可维护性：文档即运维手册**
   部署步骤、验证命令、日常运维、故障排查表全部写在 README；接口有 Knife4j 在线文档；
   代码按 `controller / service / mapper / model` 分层，异常与响应体统一，接手成本低。

7. **安全意识贯穿交付链**
   凭证零入库；接口返回统一脱敏（密码字段不出网）；逻辑删除保留可追溯性；OSS 使用 RAM 子账号 + 环境变量注入。

### 7.2 软件开发（简要）

- **全栈独立完成**：从数据库设计、接口分层、前端页面到部署上线全链路负责；
- **匹配算法优化**：将「全表扫描 + 全量编辑距离」重构为「倒排预筛 Top N + 精算 + 结果缓存」，
  5 万用户量级下匹配接口由秒级降至毫秒级，并配合标签变更主动失效缓存保证一致性；
- **前端工程化**：TypeScript 全量类型约束（`build` 阶段 `vue-tsc` 卡口）、axios 统一封装、
  环境化 API 地址、Vant 组件按需自动引入；
- **可测试性**：保留 Service 层单元测试与数据初始化测试，关键逻辑可回归。

---

## 八、后续规划

- [ ] 引入 GitHub Actions：提交即跑 `mvn test` + `npm run build`，主分支自动构建镜像；
- [ ] 增加 Prometheus + Grafana 监控（QPS、RT、缓存命中率）与告警；
- [ ] 匹配结果由「编辑距离」升级为「标签权重 + 行为反馈」的混合排序；
- [ ] 数据库读写分离与慢查询治理，补充 Flyway 版本化迁移；
- [ ] 接入 HTTPS（Nginx + 证书自动续期）与更细粒度的接口鉴权。

---

## 九、说明

- 本项目在学习实践基础上完成**架构重构、性能优化与整套部署运维方案**的设计与实施；
- 仓库不包含任何真实凭证、服务器地址或用户数据，`.env` 需自行按 `.env.example` 创建；
- 如需本地快速体验，按「5.2 Docker Compose 一键部署」执行即可，无需预先安装 MySQL / Redis。
