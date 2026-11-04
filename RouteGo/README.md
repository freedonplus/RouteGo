# RouteGo

Java 17 + Spring Boot 3.2.5 **modular monolith**.

All business domains run in one Spring Boot process, but stay in separate Maven
modules so the boundaries remain visible and the code can be extracted later if
needed.

## Modules

```text
RouteGo
├── RouteGo-common            # 基础设施：响应、异常、日志、Redis、MQ、OpenAPI
├── RouteGo-modules           # 业务域模块（普通 jar，不是独立应用）
│   ├── RouteGo-admin         # 后台身份、RBAC、字典、日志、文件、公告
│   ├── RouteGo-catalog       # 线路、分类、行程、班期、评价
│   ├── RouteGo-trade         # 订单、支付、退款
│   ├── RouteGo-customer      # C 端客户、会员、优惠券
│   └── RouteGo-operations    # 客服、定制游、统计
├── RouteGo-app               # 唯一启动模块（Spring Boot 应用）
├── RouteGo-generator         # MyBatis-Plus 代码生成器
├── sql/                      # 数据库脚本
└── doc/                      # 方案书
```

- `RouteGo-app` 依赖 5 个业务模块，统一打包、统一启动。
- 业务模块之间通过本地 Spring Bean 调用协作，不再有网关、注册中心和 Feign。
- 原有实体、Mapper、Service、Controller 全部保留，包名不变。

## Architecture

- 单进程、单端口：`8080`，上下文路径 `/api`。
- 对外路径保持 `/api/admin/**`、`/api/catalog/**`、`/api/trade/**`、
  `/api/customer/**`、`/api/operations/**`，前端 baseURL 基本不用改。
- 单数据库 `routego`，所有业务表合并在一起。
- 已移除：Spring Cloud Gateway、Nacos discovery/config、OpenFeign、
  Sentinel、LoadBalancer。
- 保留：Redis/Redisson、RocketMQ、SpringDoc、MyBatis-Plus、
  全局 Jackson、全局异常处理。
- 认证：Sa-Token 注解鉴权，Token 存 Redis；未标注鉴权注解的接口默认放行。

## Infrastructure

### Common auto-configuration

`RouteGo-common` registers the following via
`META-INF/spring/...AutoConfiguration.imports`:

| Class | Capability |
| --- | --- |
| `RouteGoOpenApiAutoConfiguration` | OpenAPI info + Bearer JWT security scheme |
| `RouteGoSpringDocAutoConfiguration` | 启动后打印 Swagger UI / OpenAPI 地址 |
| `RouteGoJacksonAutoConfiguration` | `Long` → String，统一时间格式 |
| `RouteGoMybatisPlusAutoConfiguration` | 分页、乐观锁、create/updateTime 自动填充 |
| `RouteGoRedisAutoConfiguration` | `RedisTemplate<String,Object>` JSON 序列化 + `RedisService` |
| `RouteGoRedissonAutoConfiguration` | `DistributedLock` 分布式锁封装 |
| `RouteGoRocketMqAutoConfiguration` | `EventPublisher` + 消息信封/消费基类 |

Useful classes: `RedisService`、`DistributedLock`、`EventPublisher`、
`AbstractRouteGoListener`、`PageQuery`、`PageResult`、`Result`。

### API documentation

- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`
- 上下文路径是 `/api`，直接访问 `/swagger-ui.html` 会 404。
- 启动日志会打印 banner，包含 profile、实际端口和完整文档地址。

### Authentication (Sa-Token)

- 登录：`POST /api/admin/auth/login`，body
  `{"username":"...","password":"..."}`；密码按 BCrypt 校验。
- Token：登录返回 `data.tokenValue`（兼容 Bearer）；浏览器同时收到
  `Authorization` Cookie（`HttpOnly`、`SameSite=Lax`，生产环境 `Secure`）。
- Cookie 模式：浏览器自动携带 Cookie，`CookieTokenFilter` 会把 Cookie 中的
  裸 Token 转成 `Authorization: Bearer ...` 头给 Sa-Token。
- Bearer 模式：App / 第三方仍可用 `Authorization: Bearer <token>`。
- 当前用户：`GET /api/admin/auth/me`；登出：`POST /api/admin/auth/logout`。
- `X-User-Id`：登录后由 `UserIdentityInterceptor` 从 Sa-Token 会话派生并写入响应头；
  客户端传入的 `X-User-Id` 一律忽略，不参与鉴权。
- CSRF：`CsrfOriginFilter` 对 POST/PUT/PATCH/DELETE 校验 `Origin`，
  允许来源由 `routego.security.allowed-origins` 配置，默认
  `http://localhost:5173`、`http://127.0.0.1:5173`、`http://localhost:3000`。
- 注解鉴权：`@SaCheckLogin`、`@SaCheckPermission("system:config:list")`、
  `@SaCheckRole("admin")`。**只有标注注解的接口会校验**，未标注接口默认放行。
- 角色/权限来源：`sys_user_role` / `sys_role` /
  `sys_role_permission` / `sys_permission`；`admin` / `super_admin`
  角色自动拥有通配权限 `*`。
- Token 存储在 Redis（`sa-token-redis-jackson`），key 形如
  `Authorization:login:token:<token>`。
- 不预置默认账号。使用前需要在 `sys_user` 插入一个 BCrypt 密码的用户：

```sql
INSERT INTO sys_user (username, password, nickname, status, deleted)
VALUES ('your_admin', '<BCrypt 密文>', '管理员', 1, 0);
```

### Actuator

- Health: `http://localhost:8080/api/actuator/health`
- Prometheus: `http://localhost:8080/api/actuator/prometheus`

### Redis / Redisson

Redis is required at startup because Redisson creates its connection eagerly.
The app reads the standard Spring Boot properties:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      database: ${REDIS_DATABASE:0}
      timeout: 3s
```

Set a password with `SPRING_DATA_REDIS_PASSWORD`. Do not put an empty
`spring.data.redis.password` in YAML — Redisson would send `AUTH ""` and fail.

### RocketMQ

```yaml
rocketmq:
  name-server: ${ROCKETMQ_NAME_SERVER:127.0.0.1:9876}
  producer:
    group: RouteGo-app-producer
```

If no NameServer is reachable, the app still starts; sending events fails until
RocketMQ is available. Topics/consumer groups follow `RocketMqConstants`.

### CORS

全局 CORS 在 `RouteGo-app/src/main/java/com/rg/app/config/WebMvcConfig.java`。
允许来源由 `routego.security.allowed-origins` 控制；Cookie 模式必须使用明确
Origin + `allowCredentials`，不能再用 `*`。生产环境通过
`ROUTEGO_SECURITY_ALLOWED_ORIGINS` 覆盖为实际前端域名。

## Database

单体使用一个数据库 `routego`。**首次启动应用前**先导入表结构：

```bash
mysql -u root -p < sql/routego.sql
```

Windows PowerShell:

```powershell
Get-Content .\sql\routego.sql -Raw | mysql -u root -p
```

`sql/routego.sql` 创建 36 张业务表 + 1 张 `undo_log`。原来的
`sql/routego_*.sql` 与 `sql/schema.sql` 保留为微服务版历史脚本。

## Configuration

`RouteGo-app/src/main/resources/application.yaml` 放公共配置，
环境相关配置按 profile 拆分：

| 文件 | 用途 | 默认数据库 | Redis DB |
| --- | --- | --- | --- |
| `application-dev.yaml` | 本地开发 | `routego` | 0 |
| `application-test.yaml` | 测试环境 | `routego` | 1 |
| `application-prod.yaml` | 生产环境 | `routego`（连接信息必须显式提供） | 0 |

默认激活 `dev`，切换：

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
.\mvnw.cmd -pl RouteGo-app -am spring-boot:run
```

生产环境必须提供 `MYSQL_HOST`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`、
`REDIS_HOST`、`ROCKETMQ_NAME_SERVER`；Redis 密码用
`SPRING_DATA_REDIS_PASSWORD`。

常用环境变量：

```text
SPRING_PROFILES_ACTIVE=dev
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=routego
MYSQL_USERNAME=root
MYSQL_PASSWORD=1234

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_DATABASE=0
SPRING_DATA_REDIS_PASSWORD=

ROCKETMQ_NAME_SERVER=127.0.0.1:9876
```

## Code Generator

The generator is not executed automatically. It writes into
`RouteGo-modules/RouteGo-<service>`:

```powershell
$env:MYSQL_PASSWORD = "1234"
.\mvnw.cmd -pl RouteGo-generator exec:java "-Dexec.args=admin sys_user"
```

Supported service keys: `admin`, `catalog`, `trade`, `customer`, `operations`.

## Build

```powershell
.\mvnw.cmd clean verify
```

## Run

```powershell
.\mvnw.cmd -pl RouteGo-app -am spring-boot:run
```

or:

```powershell
java -jar RouteGo-app/target/RouteGo-app-0.0.1-SNAPSHOT.jar
```

## Migration notes

- 旧的 8080–8085 微服务进程和网关需要停止，单体统一使用 `8080`。
- Nacos、Sentinel 容器不再被应用使用，可以停止。
- IDE 需要重新导入 Maven，因为 `RouteGo-services` 已改名为
  `RouteGo-modules`，`RouteGo-gateway` 已删除。
- `doc/方案书.md` 仍是微服务版设计文档，单体版以本 README 与
  `sql/routego.sql` 为准。
