# LinkDot 前端

Vue 3 + TypeScript + Vite 构建的单页应用，配套后端位于仓库根目录的 `user_match_backend`。

## 本地开发

```bash
npm ci
npm run dev      # http://localhost:5173
```

## 构建

```bash
npm run build    # 先 vue-tsc 类型校验，再输出 dist/（由 nginx 托管）
```

接口地址通过环境变量区分：`.env.development` 指向 `http://localhost:8080/api`；
`.env.production` 使用同源相对路径 `/api`，经 nginx 反向代理，避免跨域与 Cookie 问题。

完整部署步骤、目录结构与运维命令见仓库根目录 [README.md](../README.md)。
