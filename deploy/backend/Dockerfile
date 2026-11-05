# 后端镜像（运行时镜像，仅包含 JRE + 构建好的可执行 JAR，体积小、启动快）
# 构建前提：在仓库根目录执行 mvn -DskipTests package 生成
#           user_match_backend/target/user_match_backend-0.0.1-SNAPSHOT.jar
# 构建上下文为仓库根目录，见 docker-compose.yml 中的 build.context
FROM eclipse-temurin:8-jre

LABEL maintainer="Howie" \
      description="LinkDot user-match backend (Spring Boot)"

WORKDIR /app

# 时区对齐，避免日志/入库时间出现 8 小时偏差
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m"

COPY user_match_backend/target/user_match_backend-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

# 生产环境使用 prod profile；数据库/Redis/OSS 凭证全部由环境变量注入
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Duser.timezone=Asia/Shanghai -jar /app/app.jar --spring.profiles.active=prod"]
