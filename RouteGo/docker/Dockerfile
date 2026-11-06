# syntax=docker/dockerfile:1.6
# =============================================================================
# RouteGo 后端镜像（多阶段构建）
#
# 构建上下文必须是后端根目录 RouteGo/：
#   docker build -f docker/Dockerfile -t routego/backend:latest .
# 通常由 docker-compose.yml 调用，不需要手动执行。
# =============================================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# 先只拷贝各模块的 pom，命中 dependency 缓存层：只改业务代码时不会重新下依赖
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw ./
COPY RouteGo-common/pom.xml RouteGo-common/
COPY RouteGo-generator/pom.xml RouteGo-generator/
COPY RouteGo-modules/pom.xml RouteGo-modules/
COPY RouteGo-modules/RouteGo-admin/pom.xml RouteGo-modules/RouteGo-admin/
COPY RouteGo-modules/RouteGo-catalog/pom.xml RouteGo-modules/RouteGo-catalog/
COPY RouteGo-modules/RouteGo-customer/pom.xml RouteGo-modules/RouteGo-customer/
COPY RouteGo-modules/RouteGo-operations/pom.xml RouteGo-modules/RouteGo-operations/
COPY RouteGo-modules/RouteGo-trade/pom.xml RouteGo-modules/RouteGo-trade/
COPY RouteGo-app/pom.xml RouteGo-app/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -DskipTests dependency:go-offline || true

# 再拷贝源码编译。测试在 CI 阶段跑，镜像构建跳过，节省一半以上时间
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -DskipTests package \
 && find RouteGo-app/target -maxdepth 1 -name 'RouteGo-app-*.jar' ! -name '*.original' \
      -exec cp {} /workspace/app.jar \;

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre-alpine

# curl 供 HEALTHCHECK 使用，tzdata 保证 Asia/Shanghai 生效（alpine 默认不带时区库）
RUN apk add --no-cache curl tzdata \
 && addgroup -S app && adduser -S -G app app

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    SPRING_PROFILES_ACTIVE=prod \
    LOG_PATH=/app/logs \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=40 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8"

WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar

# 日志与本地存储目录，容器内以非 root 运行
RUN mkdir -p /app/logs /app/uploads && chown -R app:app /app
USER app

EXPOSE 8080 9090

# 生产 profile 把管理端点放在独立端口 9090（无 /api 前缀），健康检查走那里
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://127.0.0.1:9090/actuator/health || exit 1

# 用 sh -c 是为了让 JAVA_OPTS / APP_ARGS 里的空格与变量能展开；exec 保证 java 是 1 号进程，能收到 SIGTERM
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS $APP_ARGS -jar /app/app.jar"]
