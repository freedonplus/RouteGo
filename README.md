# RouteGo · 全国风景线路旅游电商平台

一套完整的旅游线路电商系统，包含 **后端服务**、**运营管理后台** 与 **C 端移动 App** 三部分。

后端采用 **模块化单体（Modular Monolith）**：所有业务域跑在一个 Spring Boot 进程里，
但按业务边界拆成独立 Maven 模块——部署简单，边界清晰，需要时也能单独抽出去。

```text
        ┌──────────────────┐        ┌──────────────────┐
        │  管理后台 Web     │        │   C 端 App        │
        │  Vue 3 (5173)    │        │  uni-app x (5180) │
        └────────┬─────────┘        └────────┬─────────┘
                 │        /api/**             │
                 └────────────┬───────────────┘
                              ▼
                 ┌────────────────────────┐
                 │  RouteGo-app :8080/api │  单进程 · 单端口
                 ├────────────────────────┤
                 │ admin  catalog  trade  │  5 个业务模块
                 │ customer  operations   │
                 └───────────┬────────────┘
                             ▼
              MySQL 8 · Redis · MinIO
```

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.2.5、MyBatis-Plus 3.5.7、Sa-Token 1.44、Redisson 3.27、SpringDoc 2.5 |
| 数据库 | MySQL 8.0（单库 `routego`，36 张业务表 + `undo_log`） |
| 中间件 | Redis（令牌 / 权限缓存 / 验证码）、MinIO（图片与文件对象存储） |
| 管理后台 | Vue 3.5、Vite、Element Plus 2.14、Pinia、Vue Router、Tailwind CSS |
| 移动端 | uni-app x（`.uvue` + `.uts`），一套代码出 App / H5 / 微信小程序 |
| 图表 | 自研 SVG 组件（`TrendChart` / `DonutChart`），不引入 ECharts |
| 认证 | Sa-Token：浏览器 `HttpOnly Cookie` + App `Authorization: Bearer` 双模式 |

## 目录结构

```text
RouteGo/
├── RouteGo/                        后端（Maven 多模块）
│   ├── RouteGo-common              基础设施：统一响应、异常、日志、Jackson、Redis、缓存、加密、IP 归属地
│   ├── RouteGo-modules             业务域（普通 jar，不独立启动）
│   │   ├── RouteGo-admin           后台身份、RBAC、字典、参数、日志、文件、公告
│   │   ├── RouteGo-catalog         线路、分类、行程、班期、图集、评价
│   │   ├── RouteGo-trade           订单、出行人、状态流转、支付、退款
│   │   ├── RouteGo-customer        C 端客户、会员、优惠券
│   │   └── RouteGo-operations      客服工单、定制游、统计报表
│   ├── RouteGo-app                 唯一启动模块（Spring Boot 应用）
│   ├── RouteGo-generator           MyBatis-Plus 代码生成器
│   └── README.md                   后端详细文档
├── RouteGo-admin/                  管理后台（Vue 3 + Element Plus）
├── RouteGo-mobile-app/             C 端 App（uni-app x）
├── sql/                            建表、初始化、演示数据脚本
├── doc/                            方案书、接口约定、设计规范
└── README.md                       本文件
```

## 快速开始

### 1. 环境要求

- JDK 17
- Node.js 18+
- MySQL 8.0、Redis 7
- MinIO（可选：不启动也不影响应用启动，只影响上传下载）

中间件可以用 Docker 起：

```bash
docker run -d --name mysql8  -p 3306:3306 -e MYSQL_ROOT_PASSWORD=1234 mysql:8.0 \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci
docker run -d --name redis   -p 6379:6379 redis:7
docker run -d --name minio   -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minio -e MINIO_ROOT_PASSWORD=12345678 \
  minio/minio server /data --console-address ":9001"
```

### 2. 初始化数据库（一条命令）

`sql/routego-init-all.sql` 已经把 **建表 + 后台账号/角色/权限 + 演示业务数据** 拼在一起，
新库开箱即用：

```bash
mysql -u root -p < sql/routego-init-all.sql
```

Windows（cmd）：

```bat
type sql\routego-init-all.sql | mysql -u root -p
```

导入后清一次权限缓存，避免读到旧数据：

```bash
redis-cli --scan --pattern 'admin:*' | xargs -r redis-cli del
```

> 只要空库不要演示数据？按顺序执行 `sql/routego.sql` + `sql/admin-init.sql`。
> 细节见 [`sql/README.md`](sql/README.md)。

### 3. 启动后端

```bash
cd RouteGo
./mvnw -pl RouteGo-app -am spring-boot:run          # 开发
# 或
./mvnw clean package -DskipTests
java -jar RouteGo-app/target/RouteGo-app-0.0.1-SNAPSHOT.jar
```

服务地址 `http://localhost:8080/api`（**上下文路径是 `/api`**，直接访问 8080 根路径会 404）。

### 4. 启动管理后台

```bash
cd RouteGo-admin
npm install
npm run dev          # 真实后端，http://localhost:5173
npm run dev:mock     # 纯前端 mock 演示，无需后端
```

### 5. 启动 C 端 App

用 **HBuilderX** 打开 `RouteGo-mobile-app`，运行到浏览器（`http://localhost:5180`）、
模拟器或真机。真机调试时把 `config/env.uts` 里的 API 地址改成本机局域网 IP。

## 默认账号

| 用途 | 账号 | 密码 |
| --- | --- | --- |
| 超级管理员 | `taishangxue520` | `12345678Ss` |
| 演示账号（15 个） | `13912340002` ~ `13912340016` | `Abcd1234` |

内置 5 个角色：超级管理员、系统管理员、客服、普通用户、会员用户。
C 端注册用户默认绑定**普通用户**；只有 `super_admin` 拥有通配权限 `*`，
其余角色按权限点逐条授权（权限码格式 `模块:Controller:method`）。

## 功能概览

**管理后台**：仪表盘（KPI / 30 天趋势 / 模块用量 / 异常登录 / 服务探针）、线路与班期、
订单全流程、会员与优惠券、客服工单与对话（文字 / 图片 / 自动分配规则 / 新消息提醒）、定制游、
系统管理（用户 / 角色 / 菜单 / 权限 / 字典 / 参数 / 公告 / 文件 / 操作日志 / 登录日志）。

**C 端 App**：启动页、首页、线路列表与详情、确认订单、我的订单、订单详情、
优惠券包、在线客服（发文字与照片、未读提醒）、登录注册、个人中心、定制需求提交。

**几个实现要点**

- **图片上传**：拖拽或点击选本地文件，直传后端再进 MinIO，不填 URL；C 端客服会话支持拍照 / 相册发图（jpg / png / webp，单张 5MB 内）。
- **临时签名链接**：`<img>` 带不上登录态，文件接口额外返回 `previewUrl`
  （HMAC-SHA256 签名 + 2 小时过期），无登录态也能加载，篡改或过期返回 403；
  写入口会把临时链接还原成标准下载地址，签名 URL 不会落库，聊天记录里的图片每次读取重新签发，历史图片不会过期。
- **客服会话**：客户可发起咨询、追加文字 / 图片、结束会话；客服在后台同一时间线回复，
  首次回复自动认领工单；双方各有未读数，打开会话即清零；客户在「已解决」会话里追问会自动重开，已关闭为终态。
- **工单自动分配**：按「工单类型 / 优先级」圈定一组客服，策略选轮询或「当前在处理最少」，
  规则按排序命中的第一条生效；新建工单（客户咨询或后台建单）未手动指派时自动落到人，
  候选客服全部停用则工单留在未指派池。命中规则记在 `cs_ticket.assign_rule_id`，可追溯。
- **实时推送（SSE）**：客服工单页与后台之间维持一条 `text/event-stream` 长连接，客户发消息、
  客服读消息、工单改状态都会即时推过去，前端不再靠定时器轮询——列表页原来 15 秒的等待直接变秒级。
  `EventSource` 带不上 `Authorization` 头，所以先用登录态换一张 **60 秒有效、用后即删**的票据再建连；
  票据是一次性的，而 `EventSource` 断线会拿同一张废票自动重试（401 死循环），因此前端主动接管
  `onerror`：关连接 → 重新换票 → 1/2/5/10/30 秒退避重连。推送断开时自动退回 30 秒兜底轮询，
  恢复后立刻切回，页面右上角有「实时 / 轮询」状态指示灯。
- **新消息提醒**：未读总数上涨就响一声（WebAudio 合成，可一键关掉）、弹通知、浏览器标题带未读角标；
  未读行整行浅红高亮 + 左侧红条 +「N 条待回」角标，可一键「只看待回复」。对话抽屉内新消息以悬浮
  气泡提示，客服正在翻历史时不会被强制拉到底部。

- **字段加密**：手机号、证件号 AES-GCM 加密入库，等值查询走 HMAC 盲索引。
- **权限缓存**：按用户缓存到 Redis，key 带全局版本号，角色变更只删版本号即可整体失效。
- **下单幂等**：请求头带 `Idempotency-Key`，同键重复提交返回同一张订单；未传时按「会员 + 班期 + 人数」30 秒窗口去重。
- **余位并发**：班期扣减走条件更新 + Redisson 锁（锁在事务外层），不会超卖。

## 接口文档与监控

| 用途 | 地址 |
| --- | --- |
| Swagger UI | `http://localhost:8080/api/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/api/v3/api-docs` |
| 健康检查 | `http://localhost:8080/api/actuator/health` |
| Prometheus 指标 | `http://localhost:8080/api/actuator/prometheus` |

接口路径按模块划分：`/api/admin/**`、`/api/catalog/**`、`/api/trade/**`、
`/api/customer/**`、`/api/operations/**`；C 端为 `/api/app/**`。

## 常用配置

生产环境请用环境变量覆盖以下默认值（详见 `RouteGo/RouteGo-app/src/main/resources/application.yaml`）：

| 变量 | 说明 | 默认 |
| --- | --- | --- |
| `MINIO_ENDPOINT` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | 对象存储连接 | `127.0.0.1:9000` / `minio` / `12345678` |
| `ROUTEGO_STORAGE_TYPE` | `minio` 或 `local`（本地磁盘兜底） | `minio` |
| `ROUTEGO_TEMP_URL_SECRET` | 临时预览链接签名密钥，**生产必须改** | 开发占位值 |
| `ROUTEGO_CRYPTO_AES_KEY` / `ROUTEGO_CRYPTO_HMAC_KEY` | 敏感字段加密密钥，**生产必须改且两把不能相同** | 开发占位值 |
| `ROUTEGO_SECURITY_ALLOWED_ORIGINS` | CORS 允许来源 | `localhost:5173` 等 |
| `SPRING_DATA_REDIS_PASSWORD` | Redis 密码（无密码时不要写空值） | 空 |

### 反向代理与 SSE

客服推送走 `Server-Sent Events`，放在 nginx 后面时有两处必须改，否则表现是「消息攒着才到」：

```nginx
location /api/operations/cs/stream {
    proxy_pass         http://127.0.0.1:8080;
    proxy_http_version 1.1;
    proxy_set_header   Connection '';
    proxy_set_header   Host $host;
    proxy_buffering    off;      # 关键：不关会让 SSE 变成批量到货
    proxy_read_timeout 1h;       # 默认 60s 会周期性掐断长连接
    gzip               off;
}
```

后端已主动下发 `X-Accel-Buffering: no`，并每 20 秒发一次心跳注释帧；
即使代理层仍把连接掐断，前端也会自动换票重连，并在断开期间退回 30 秒兜底轮询。

## 测试

```bash
cd RouteGo && ./mvnw test        # 后端 144 例
cd RouteGo-admin && npx vitest run   # 前端 24 例
```

## 文档

| 文档 | 内容 |
| --- | --- |
| [`doc/方案书.md`](doc/方案书.md) | 总体方案：架构、技术选型、功能、数据库、接口、部署、交付清单 |
| [`sql/README.md`](sql/README.md) | 初始化脚本清单与数据基线 |
| [`RouteGo/README.md`](RouteGo/README.md) | 后端模块、鉴权、存储、缓存、配置细节 |
| [`RouteGo-admin/README.md`](RouteGo-admin/README.md) | 管理端环境变量与开发约定 |

## 已知限制

- 支付为**模拟支付**（保留完整支付/退款流水表结构），未对接真实微信支付。
- 短信通道默认走日志实现，未接云厂商；第三方登录需自行配置 AppID / Secret。
- RocketMQ 已下线（`routego.mq.enabled=false`），订单超时与统计汇总由定时任务承担。
- 代码生成器仅用于开发期，不参与运行时。
- 接口级限流（登录、导出）尚未实现，目前只有验证码与短信发送的频率限制。
