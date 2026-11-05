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
| `RouteGoCacheAutoConfiguration` | `CacheHelper` + 空值哨兵（`__RG_CACHE_NULL__`） |
| `RouteGoRocketMqAutoConfiguration` | `EventPublisher` + 消息信封/消费基类（**一期不启用**） |
| `RouteGoSmsAutoConfiguration` | 短信通道，默认日志实现，可换云厂商 |
| `RouteGoIpLocationAutoConfiguration` | ip2region IP 归属地解析 |
| `RouteGoCryptoAutoConfiguration` | AES-GCM 字段加密 + HMAC 盲索引 |

Useful classes: `RedisService`、`DistributedLock`、`CacheHelper`、`EventPublisher`、
`PageQuery`、`PageResult`、`Result`。

> RocketMQ：一期**运行时不启用**。`routego.mq.enabled=false`（默认），
> `application-dev/test.yaml` 中的 `rocketmq:` 配置块已注释；`EventPublisher`
> 与监听基类保留，供二期恢复使用。订单超时、统计汇总等异步需求由定时任务承担。

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
- 注解鉴权：`@SaCheckLogin`、`@SaCheckPermission("admin:SysConfig:getByKey")`、
  `@SaCheckRole("admin")`。**只有标注注解的接口会校验**，未标注接口默认放行。
- 角色/权限来源：`sys_user_role` / `sys_role` /
  `sys_role_permission` / `sys_permission`；**只有 `super_admin` 拥有通配权限 `*`**，
  其余角色（含 `admin`）按权限点逐条授权。权限码格式 `模块:Controller:method`。
- 权限缓存：按用户缓存到 Redis，key 带全局版本号；角色/权限变更只需删版本号 key
  即可一次性失效，避免脏读。
- 内置 5 个角色：`super_admin`(1)、`admin`(2)、`support`(3)、`normal_user`(4)、
  `member_user`(5)。C 端注册用户默认绑定 `normal_user`。
- Token 存储在 Redis（`sa-token-redis-jackson`），key 形如
  `Authorization:login:token:<token>`。
- 账号由初始化脚本预置（见 `sql/README.md`）：
  - 超级管理员 `taishangxue520` / `12345678Ss`
  - 演示账号 `13912340002` ~ `13912340016` / `Abcd1234`
- 全新环境执行 `mysql -u root -p < sql/routego-init-all.sql` 即可获得账号、角色、
  角色绑定、195 个权限点、30 条菜单与演示业务数据。
- 也可以手工建号（BCrypt 密文需自行生成）：

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

**已下线**（消息域无调用方）：`routego.mq.enabled=false` 且 dev/test profile 不再
配置 `rocketmq.name-server`，本地无需启动 RocketMQ。需要重新启用时：

1. 恢复 `application-dev.yaml` 中被注释的 `rocketmq:` 配置块；
2. 设置 `routego.mq.enabled=true`。

Topics/consumer groups follow `RocketMqConstants`.

### 文件存储（MinIO）

上传的图片/文件默认存 **MinIO 对象存储**（`routego.storage.type=minio`），
bucket 首次上传时自动创建；`type=local` 时回落到本地磁盘 `./uploads`。
下载统一走后端 `/admin/system/file/{id}/download`，图片按真实 MIME 内联返回。

```yaml
routego:
  storage:
    type: minio
    minio:
      endpoint: ${MINIO_ENDPOINT:http://127.0.0.1:9000}
      access-key: ${MINIO_ACCESS_KEY:minio}
      secret-key: ${MINIO_SECRET_KEY:12345678}
      bucket: ${MINIO_BUCKET:routego}
```

App 启动不依赖 MinIO 存活；MinIO 不可用只影响上传/下载动作本身。

### CORS

全局 CORS 在 `RouteGo-app/src/main/java/com/rg/app/config/WebMvcConfig.java`。
允许来源由 `routego.security.allowed-origins` 控制；Cookie 模式必须使用明确
Origin + `allowCredentials`，不能再用 `*`。生产环境通过
`ROUTEGO_SECURITY_ALLOWED_ORIGINS` 覆盖为实际前端域名。

## Database

单体使用一个数据库 `routego`。**首次启动应用前**先导入初始化脚本，
`sql/routego-init-all.sql` 一个文件搞定建表 + 后台账号/角色/权限 + 演示业务数据：

```bash
mysql -u root -p < sql/routego-init-all.sql
```

Windows 下用管道导入：

```bat
type sql\routego-init-all.sql | mysql -u root -p
```

只需要空库时，可按顺序只跑前两步：

```bash
mysql -u root -p < sql/routego.sql              # 建库 + 36 张业务表 + undo_log
mysql -u root -p routego < sql/admin-init.sql   # 账号 / 角色 / 菜单 / 权限点 / 字典 / 参数 / 公告
mysql -u root -p routego < sql/demo-data.sql    # 可选：演示业务数据（幂等）
```

`routego-init-all.sql` 是上面三个文件按顺序拼接的产物，改完源文件后执行
`cd sql && sh build-init-all.sh` 重新生成。原来的 `sql/routego_*.sql` 与
`sql/schema.sql` 保留为微服务版历史脚本。

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
`REDIS_HOST`；Redis 密码用 `SPRING_DATA_REDIS_PASSWORD`。

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

MINIO_ENDPOINT=http://127.0.0.1:9000
MINIO_ACCESS_KEY=minio
MINIO_SECRET_KEY=12345678
MINIO_BUCKET=routego
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

## Admin module (implemented)

后台模块已按 `doc/superpowers/specs/2026-11-04-admin-module-design.md` 落地。

### Endpoints

| 模块 | 路径 |
| --- | --- |
| 认证 | `GET /admin/auth/captcha`、`POST /admin/auth/login`、`POST /admin/auth/logout`、`GET /admin/auth/me`、`PUT /admin/auth/password` |
| 用户 | `/admin/system/user`（page、详情、增删改、status、password、roles） |
| 角色 | `/admin/system/role`（page、详情、增删改、status、menus、permissions、users） |
| 菜单 | `/admin/system/menu`（tree、user-tree、详情、增删改、status、visible） |
| 权限点 | `/admin/system/permission`（page、grouped、详情、增删改、status） |
| 字典 | `/admin/system/dict/type`、`/admin/system/dict/data` |
| 参数 | `/admin/system/config`（page、key、详情、增删改、batch） |
| 公告 | `/admin/system/notice`（page、published、详情、增删改、publish/unpublish/top） |
| 文件 | `/admin/system/file`（upload、page、详情、download、delete） |
| 日志 | `/admin/system/oper-log`、`/admin/system/login-log` |
| 第三方绑定 | `/admin/system/user-oauth` |

### 登录与验证码

1. `GET /api/admin/auth/captcha` 返回 `{uuid, image}`，`image` 是 4 位字符 PNG 的 Base64。
2. 登录请求体：`{username, password, captchaUuid, captchaCode}`。
3. 验证码在 Redis 保存 2 分钟且只能使用一次；登录失败 5 次锁定 10 分钟。

### 缓存 key 规范

格式 `[模块]:[表/控制器]:[方法]:[参数]`，全部定义在
`com.rg.common.constant.CacheKeyConstants`，例如：

- `admin:SysUser:getById:1`
- `admin:SysDictData:listByType:sys_user_status`
- `admin:SysConfig:getByKey:site.name`
- `lock:cache:rebuild:{key}`

统一使用 `CacheHelper.getOrLoad`：空值缓存 60 秒（防穿透）、Redisson 互斥锁重建（防击穿）、
TTL ±10% 抖动（防雪崩）。写操作在事务提交后删除相关 key。

### 脱敏

`@Sensitive(PHONE/EMAIL/NAME/ID_CARD/BANK_CARD)` 加在 VO 字段上，仅影响 JSON 输出；
数据库与缓存保持明文。编辑接口收到带 `*` 的掩码值时视为不修改。

### 初始化

```bash
# 一键：建表 + 后台数据 + 演示业务数据（幂等，可重复执行）
mysql -u root -p < sql/routego-init-all.sql

# 只更新权限码：直接重跑 init（会清理旧权限点并重建）
mysql -u root -p routego < sql/admin-init.sql

# 想彻底清空旧菜单/权限目录时，先 reset 再 init
mysql -u root -p routego < sql/admin-reset.sql
mysql -u root -p routego < sql/admin-init.sql
```

`admin-init.sql` 创建 5 个内置角色、195 个权限点、30 个菜单与授权，并写入内置管理员
`taishangxue520 / 12345678Ss`（超级管理员）；`demo-data.sql` 另外补 15 个演示注册账号
（`13912340002` ~ `13912340016`，密码 `Abcd1234`，绑定普通用户 / 会员用户角色），
与演示会员同手机号对应，可直接在后台和 App 两个入口登录。`admin-reset.sql` 会清空
`sys_role_menu` 和 `sys_role_permission`，执行后需要重新给角色勾选权限。

用户体系说明（后台账号 `sys_user` 与 App 会员 `customer` 的关系、订单归属）见
`sql/README.md` 的「用户体系与订单归属」一节。

### 测试

```bash
cd RouteGo
PATH="/usr/bin:$PATH" JAVA_HOME="/d/Program Files/java/java21" \
  ./mvnw -pl RouteGo-modules/RouteGo-admin -am test
```
