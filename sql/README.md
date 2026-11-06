# RouteGo 数据库

单体多模块架构，所有业务共用一个 `routego` 数据库。
MySQL 8.0，统一字符集 `utf8mb4`，排序规则 `utf8mb4_general_ci`。

## 初始化（一键）

**推荐：一个文件搞定建表 + 后台数据 + 演示业务数据**，新库开箱即用。

命令行直接导入：

```bash
mysql -u root -p < sql/routego-init-all.sql
```

Windows 下用管道导入：

```bat
type sql\routego-init-all.sql | mysql -u root -p
```

`routego-init-all.sql` 由三个文件按顺序拼接（`sql/build-init-all.sh` 可随时重新生成）：

| 顺序 | 文件 | 内容 | 幂等性 |
| --- | --- | --- | --- |
| 1 | `routego.sql` | 建库 `routego` + 36 张业务表 + `undo_log` | `CREATE ... IF NOT EXISTS` |
| 2 | `admin-init.sql` | 账号 / 角色 / 菜单 / 权限点 / 字典 / 参数 / 公告 | 覆盖式，可重跑 |
| 3 | `demo-data.sql` | 演示业务数据（线路、会员、订单、工单、统计等） | `INSERT IGNORE`，可重跑 |

> 执行完清一次 Redis 权限缓存，否则会读到旧权限：
> `redis-cli --scan --pattern 'admin:*' | xargs -r redis-cli del`

### 分步初始化（按需）

只想要空库、不要演示数据时，按顺序执行前两个文件即可：

```bash
mysql -u root -p < sql/routego.sql              # 1) 建表
mysql -u root -p routego < sql/admin-init.sql   # 2) 后台账号 / 角色 / 权限
mysql -u root -p routego < sql/demo-data.sql    # 3) 可选：演示业务数据
```

修改任一源文件后，重新生成一键脚本：

```bash
cd sql && sh build-init-all.sh
```

## 后台权限数据（重置 + 初始化）

`routego.sql` 只建表、不写数据。账号 / 角色 / 菜单 / 权限点 / 字典 / 参数 / 公告
由下面两个脚本负责，**必须按顺序执行**：

```bash
# 1) 清空后台权限数据（账号、角色、菜单、权限点、字典、参数、公告、日志）
mysql -u root -p routego < sql/admin-reset.sql

# 2) 写入标准初始化数据（幂等，可重复执行）
mysql -u root -p routego < sql/admin-init.sql
```

> 执行完要清一次 Redis 权限缓存，否则会读到旧权限：
> `redis-cli --scan --pattern 'admin:*' | xargs -r redis-cli del`

- 只是升级权限码、想保留账号时，**不要**执行 `admin-reset.sql`，直接重跑 `admin-init.sql`。
- `admin-init.sql` 覆盖式管理：权限点 ID 1~195、菜单 ID 100~199、角色 ID 1~5，
  重跑会把这几类数据恢复成脚本定义；用户自建的权限点（ID > 195）不受影响。

### 内置账号

| 账号 | 密码 | 角色 | 手机号 | 邮箱 |
| --- | --- | --- | --- | --- |
| `taishangxue520` | `12345678Ss` | 超级管理员 | 18807420597 | 2120387318@qq.com |

账号支持「用户名 / 手机号 / 邮箱」三种方式登录。密码为 BCrypt 密文，
**正式环境请首次登录后立即修改**；如需重新生成密文，用项目里的 `PasswordEncoder`
或任意 BCrypt(strength=10) 工具生成后替换 `sys_user.password`。

### 内置角色

共 5 个（2026-11-05 精简）。普通用户 / 会员用户是 App 端角色，不分配后台菜单与权限点；
注册接口（`POST /admin/auth/register`）创建的账号默认绑定「普通用户」。

| ID | 角色 | 编码 | 权限范围 |
| --- | --- | --- | --- |
| 1 | 超级管理员 | `super_admin` | 全部权限（鉴权时直接返回通配符 `*`），不可停用/删除 |
| 2 | 系统管理员 | `admin` | 全部权限，但不含「权限点」「菜单」的增删改（防误改鉴权骨架） |
| 3 | 客服 | `support` | 用户资料查询与维护、手机号换绑、券包查询与核销作废、附件下载、公告查看、订单代录与出行人维护、退款申请、**工单全流程与定制需求跟进（含报价采纳）** |
| 4 | 普通用户 | `normal_user` | 注册默认角色（App 端），无后台权限 |
| 5 | 会员用户 | `member_user` | 预留会员体系扩展，无后台权限 |

### 权限点

195 个接口级权限点，命名规则 `<域>:<Controller>:<method>`（后台管理为 `admin:`，
业务域为 `catalog:` / `trade:` / `customer:` / `operations:`），与 Controller 上的
`@SaCheckPermission` 一一对应；每个权限点在 `sys_permission.description` 里都有中文说明，
后台「系统管理 - 权限点」与角色授权抽屉都能看到。

- 后台管理：74 个（ID 1~74），覆盖用户、角色、菜单、权限点、字典、参数、公告、文件与日志
- 线路管理：40 个（ID 75~114），覆盖线路、分类、行程、图片、标签、标签绑定、班期与评价
- 订单管理：23 个（ID 115~137），覆盖订单、出行人、状态流水、支付记录与退款（申请 / 审核分开授权）
- 会员管理：11 个（ID 138~148），覆盖会员查询、代注册、资料、手机号换绑、密码、等级、积分与券包
- 优惠券模板：8 个（ID 149~156），覆盖模板维护、启停、批量发券与删除
- 用户优惠券：7 个（ID 157~163），覆盖券查询、券包、核销、作废、删除与过期刷新
- 客服工单：9 个（ID 164~172），覆盖受理、编辑、指派、优先级、状态流转、删除与看板统计
- 工单消息：3 个（ID 173~175），覆盖分页、整段对话与客服回复
- 定制需求：7 个（ID 176~182），覆盖受理、编辑、指派、人工成交/关闭与删除
- 定制报价：8 个（ID 183~190），覆盖版本查询、新建、改价、采纳、作废与删除
- 经营统计：5 个（ID 191~195），覆盖分页、单日明细、区间汇总、趋势与重算

### 后台菜单与页面

30 个菜单（5 个目录 + 25 个页面），ID 100~199 由脚本固定占用；页面路径与前端
`src/router/menu-components.js` 的组件 key 严格对应，改路径必须两边一起改。

| 目录 | 页面 | 路径 | 组件 key | 入口权限码 |
| --- | --- | --- | --- | --- |
| 系统管理 (100) | 用户 / 角色 / 菜单 / 权限点 / 字典 / 参数 / 公告 / 文件 / 操作日志 / 登录日志 | `/system/*` | `system/*/index` | `admin:*` |
| 线路管理 (111) | 线路列表 | `/catalog/route` | `catalog/route/index` | `catalog:Route:page` |
| | 线路分类 | `/catalog/category` | `catalog/category/index` | `catalog:RouteCategory:tree` |
| | 线路标签 | `/catalog/tag` | `catalog/tag/index` | `catalog:RouteTag:page` |
| | 出发班期 | `/catalog/departure` | `catalog/departure/index` | `catalog:RouteDeparture:page` |
| | 线路评价 | `/catalog/review` | `catalog/review/index` | `catalog:RouteReview:page` |
| 订单管理 (120) | 订单列表 | `/trade/order` | `trade/order/index` | `trade:Orders:page` |
| | 退款管理 | `/trade/refund` | `trade/refund/index` | `trade:RefundRecord:page` |
| | 支付流水 | `/trade/payment` | `trade/payment/index` | `trade:PaymentRecord:page` |
| 会员管理 (130) | 会员列表 | `/customer/member` | `customer/customer/index` | `customer:Customer:page` |
| | 优惠券模板 | `/customer/coupon-template` | `customer/coupon-template/index` | `customer:CouponTemplate:page` |
| | 用户优惠券 | `/customer/coupon` | `customer/coupon/index` | `customer:Coupon:page` |
| 运营中心 (140) | 客服工单 | `/operations/ticket` | `operations/ticket/index` | `operations:CsTicket:page` |
| | 定制需求 | `/operations/requirement` | `operations/requirement/index` | `operations:CustomRequirement:page` |
| | 定制报价 | `/operations/quote` | `operations/quote/index` | `operations:CustomQuote:page` |
| | 经营统计 | `/operations/stat` | `operations/stat/index` | `operations:StatDaily:page` |

前端路由只保留一条受约束的动态段 `:module([a-z]+)/:pathMatch(.*)*`，由后端菜单驱动，
新增业务模块只要补菜单记录 + 组件 key，不用改路由表。

## 业务模块接口进度

| 模块 | 表 | 接口状态 | 权限码前缀 |
| --- | --- | --- | --- |
| 线路 catalog | route / route_category / route_itinerary / route_image / route_tag / route_tag_rel / route_departure / route_review | 已完成（35 个接口） | `catalog:` |
| 订单 trade | orders / order_traveler / order_status_log / payment_record / refund_record | 已完成（24 个接口） | `trade:` |
| 客户 customer | customer / coupon / coupon_template | 已完成（26 个接口） | `customer:` |
| 运营 operations | cs_ticket / cs_message / custom_requirement / custom_quote / stat_daily | 已完成（32 个接口） | `operations:` |

以上四个业务域的后台页面已全部实现（16 个页面 + 5 个业务抽屉/弹窗），
入口见上一节「后台菜单与页面」，前端代码在 `RouteGo-admin/src/views/{catalog,trade,customer,operations}`。

线路模块接口一览（前缀 `/api/catalog`）：

- `route`：`GET /page`、`GET /{id}`、`POST /`、`PUT /{id}`、`PUT /{id}/status`、`DELETE /{id}`
- `route-category`：`GET /tree`、`GET /{id}`、`POST /`、`PUT /{id}`、`PUT /{id}/status`、`DELETE /{id}`
- `route-itinerary`：`GET /list/{routeId}`、`POST /`、`PUT /{id}`、`DELETE /{id}`、`PUT /batch/{routeId}`
- `route-image`：`GET /list/{routeId}`、`POST /`、`DELETE /{id}`
- `route-tag`：`GET /page`、`GET /list`、`POST /`、`PUT /{id}`、`PUT /{id}/status`、`DELETE /{id}`
- `route-tag-rel`：`GET /list/{routeId}`、`PUT /{routeId}`、`DELETE /{routeId}/{tagId}`
- `route-departure`：`GET /page`、`GET /list/{routeId}`、`POST /`、`PUT /{id}`、`PUT /{id}/status`、`DELETE /{id}`
- `route-review`：`GET /page`、`GET /{id}`、`PUT /{id}/reply`、`PUT /{id}/status`、`DELETE /{id}`

订单模块接口一览（前缀 `/api/trade`）：

- `orders`：`GET /page`、`GET /{id}`、`POST /`、`PUT /{id}`、`POST /{id}/cancel`、
  `POST /{id}/pay`、`POST /{id}/start`、`POST /{id}/complete`、`DELETE /{id}`
- `order-traveler`：`GET /list/{orderId}`、`POST /{orderId}`、`PUT /{id}`、`DELETE /{id}`、`PUT /batch/{orderId}`
- `order-status-log`：`GET /page`、`GET /list/{orderId}`
- `payment-record`：`GET /page`、`GET /list/{orderId}`
- `refund-record`：`GET /page`、`GET /{id}`、`GET /list/{orderId}`、`POST /{orderId}`、`PUT /{id}/audit`

订单状态机（服务端唯一入口 `OrderStateMachine`，非法流转直接 409）：

```
0 待支付 ──支付──> 1 已支付 ──开始行程──> 2 出行中 ──完成──> 3 已完成
   └──取消──> 4 已取消
1 已支付 ──申请退款──> 5 退款中 ──通过（全额）──> 6 已退款
                          └──驳回 / 部分退款──> 1 已支付
```

连带更新：下单占班期库存（条件 UPDATE 防超卖）；支付累加线路销量与客户消费；
取消回补库存；全额退款回补库存并回退销量与消费。已支付订单不允许直接取消，必须走退款。

客户模块接口一览（前缀 `/api/customer`）：

- `customer`：`GET /page`、`GET /{id}`、`POST /`、`PUT /{id}`、`PUT /{id}/phone`、
  `PUT /{id}/password`、`PUT /{id}/status`、`PUT /{id}/level`、`POST /{id}/points`、
  `GET /{id}/coupons`、`DELETE /{id}`
- `coupon-template`：`GET /page`、`GET /list`、`GET /{id}`、`POST /`、`PUT /{id}`、
  `PUT /{id}/status`、`POST /grant`、`DELETE /{id}`
- `coupon`：`GET /page`、`GET /{id}`、`GET /list/{customerId}`、`PUT /{id}/use`、
  `PUT /{id}/revoke`、`DELETE /{id}`、`POST /expire-refresh`

会员与优惠券的关键规则：

- **手机号加密落库**：`phone` 存 AES/GCM 密文，另存 HMAC 盲索引 `phone_index` 支持精确查询，
  接口出口一律脱敏（`138****8000`）；手机号唯一性校验包含逻辑删除的行（唯一索引不含 `deleted`）。
- **券的过期时间是算出来的**：`coupon` 表不存过期时间，按模板规则推导——
  固定区间取模板 `end_time`，领取后 N 天取 `receive_time + valid_days`；
  接口返回 `expireTime` 与 `usable`，并提供 `expire-refresh` 把已过期券批量置为「已过期」。
- **发券不超发**：`received_count < total_count` 的条件 UPDATE 原子占量，`total_count = 0` 表示不限量；
  批量发券整批事务，任一会员不存在/已禁用/券已发完都会整体回滚。
- **核销只成功一次**：条件 UPDATE `status = 0 → 1`；传订单 ID 时校验订单归属、未取消未退款、
  金额达到使用门槛；核销只建立「券 ↔ 订单」关联，不改订单金额（金额计算属于交易域）。
- **模板不允许删有发放记录的**：已发过券的模板只能停用；已使用的券是核销凭证，也不允许删除。

运营模块接口一览（前缀 `/api/operations`）：

- `cs-ticket`：`GET /page`、`GET /{id}`、`POST /`、`PUT /{id}`、`PUT /{id}/assign`、
  `PUT /{id}/priority`、`PUT /{id}/status`、`DELETE /{id}`、`GET /statistics`
- `cs-message`：`GET /page`、`GET /list?ticketId=`、`POST /`
- `custom-requirement`：`GET /page`、`GET /{id}`、`POST /`、`PUT /{id}`、`PUT /{id}/assign`、
  `PUT /{id}/status`、`DELETE /{id}`
- `custom-quote`：`GET /page`、`GET /{id}`、`GET /list?requirementId=`、`POST /`、
  `PUT /{id}`、`PUT /{id}/accept`、`PUT /{id}/invalidate`、`DELETE /{id}`
- `stat-daily`：`GET /page`、`GET /{statDate}`、`GET /summary`、`GET /trend`、`POST /rebuild`

工单状态机（服务端唯一入口 `TicketStateMachine`，非法流转直接 409）：

```
0 待处理 ──指派/回复──> 1 处理中 ──解决──> 2 已解决 ──归档──> 3 已关闭
   └──────── 直接解决 ────────> 2
   └──────── 直接关闭 ────────> 3
                          2 ──重新打开──> 1
```

- 指派即认领：待处理工单指派处理人后自动进入「处理中」；客服首次回复同样会推进状态。
- 状态流转带乐观锁版本号，备注会作为一条「客服」消息写入工单对话，便于复盘。
- 工单号 `TK + yyyyMMdd + 5 位随机`，唯一性校验包含已逻辑删除的记录。
- 工单删除会连带物理清理其消息（消息无逻辑删除列，避免留下孤儿数据）。

定制需求与报价的联动：

```
需求 0 待跟进 ──新建报价──> 1 已报价 ──采纳报价──> 2 已成交 ──> 3 已关闭
        └──────── 线下成交（人工）────────> 2
```

- 采纳一版报价会把同需求下其它「待确认」报价自动置为「已失效」，整体一个事务；
  条件 UPDATE `status = 0 → 1` 保证并发下只有一次能成功。
- 人工状态接口只接受 2（线下成交）与 3（关闭）；已有报价时不允许手工置成交，必须走采纳流程。
- 需求联系电话加密落库、出口脱敏；表上没有盲索引列，因此**不支持按手机号检索**。
- 需求删除会连带清理其报价版本；已成交的需求不允许删除，只能关闭。

经营统计口径（`stat_daily` 由凌晨定时任务生成，后台可手工重算）：

| 字段 | 口径 |
| --- | --- |
| `order_count` | 当日 `orders.create_time` 的新建订单数，含已取消 |
| `paid_order_count` | 当日 `payment_record.pay_time` 的成功支付订单数（去重） |
| `sales_amount` | 当日成功支付流水金额合计，分多次支付按流水累加 |
| `refund_amount` | 当日 `refund_record.update_time` 且状态为已退款的金额合计 |
| `new_customer_count` | 当日 `customer.register_time` 的新增会员数 |

重算按天 upsert（`stat_date` 唯一），幂等可重复执行，单次最多 366 天；没有数据的日子写 0，
避免折线图断点。区间汇总接口不读 `stat_daily`，直接聚合原始流水，报表漏跑也不会算错。

启动后可在 `http://localhost:8080/api/swagger-ui.html` 直接调试。

## 结构

- 1 个库：`routego`
- 36 张业务表 + 1 张 Seata AT `undo_log` 基础表
- 按原业务域分组：系统权限 15、线路 8、交易 5、客户 3、运营 5
- 不建物理外键，跨业务域关系由应用层保证
- 业务表统一包含审计字段（`create_by/create_time/update_by/update_time`）
  与逻辑删除字段 `deleted`
- `version` 乐观锁字段只加在需要并发控制的表上

## 脚本清单

| 文件 | 用途 | 是否需日常执行 |
| --- | --- | --- |
| `routego-init-all.sql` | **一键初始化**（建表 + 后台数据 + 演示数据） | 新库首次部署 |
| `routego.sql` | 建库 + 36 张业务表 + `undo_log` | 分步初始化第 1 步 |
| `admin-init.sql` | 账号 / 角色 / 菜单 / 权限点 / 字典 / 参数 / 公告 | 分步初始化第 2 步、权限升级 |
| `demo-data.sql` | 演示业务数据（幂等） | 可选，分步第 3 步 |
| `admin-reset.sql` | 清空后台权限数据 | 仅在彻底重置时 |
| `build-init-all.sh` | 由上面 3 个文件重新合成一键脚本 | 修改源文件后 |
| `2026-11-05-roles-simplify.sql` | 角色 8→5 迁移（已在库执行，仅存档） | 否 |
| `2026-11-05-add-login-log-location.sql` | 登录日志补 IP 归属地字段（已执行，仅存档） | 否 |
| `upgrade-cs-chat.sql` | 客服会话增强：`cs_ticket` 补未读数与最后消息字段，并回填历史会话摘要（可重复执行） | 已有库升级时执行一次 |
| `upgrade-cs-assign-rule.sql` | 工单自动分配：新建 `cs_assign_rule`、`cs_ticket` 补 `assign_rule_id`、补齐 5 个权限点（可重复执行） | 已有库升级时执行一次 |
| `upgrade-cs-assign-permission.sql` | 指派处理人权限收紧：仅超级管理员/系统管理员可指派，客服角色移除该权限（可重复执行） | 已有库升级时执行一次 |
| `backup/` | 重大变更前的整库备份 | 回滚用 |

微服务时代的 `routego_*.sql`、`schema.sql` 等历史脚本已废弃，单体部署只用上表文件。

## 敏感字段加密

- `orders.contact_phone`、`order_traveler.id_card / phone` 在库里以 AES/GCM 密文保存，
  同一明文每次加密结果不同；需要等值查询的手机号额外存一列 HMAC-SHA256 盲索引
  （`orders.contact_phone_index`），查询时先算盲索引再匹配。
- 密钥通过 `routego.crypto.*` 注入：开发环境有默认值，生产必须用环境变量
  `ROUTEGO_CRYPTO_AES_KEY`（Base64，解码后 32 字节）与 `ROUTEGO_CRYPTO_HMAC_KEY`
  覆盖，缺失时应用直接启动失败，避免误用示例密钥。
- 接口返回的手机号、证件号一律脱敏（`138****8000` / `4301**********1234`）。

## 演示数据

`demo-data.sql` 内置一整套自洽的演示数据（2026-11-05 通过真实接口生成，再校正时间分布），
已包含在一键初始化中，也可单独导入（`INSERT IGNORE` 幂等）。

| 域 | 数据 | 说明 |
| --- | --- | --- |
| 线路 | 7 分类 / 8 标签 / 10 条线路 / 44 段行程 / 30 张图 / 40 个班期 | 班期含 10 月已出发与 11-12 月可售 |
| 评价 | 6 条（挂已完成订单） | 3 条带官方回复 |
| 会员 | 36 名（等级 1~4） | 手机号 AES 加密 + 盲索引 |
| 优惠券 | 3 个模板 / 116 张券 | 满减、无门槛、95 折三种 |
| 订单 | 37 笔：待支付 4 / 已支付 14 / 出行中 6 / 已完成 9 / 已取消 3 / 退款中 1 | 85 名出行人、30 笔支付流水、4 笔退款 |
| 工单 | 8 个工单 / 20 条消息 | 待处理 0、处理中 5、已解决 2、已关闭 1 |
| 定制游 | 6 个需求 / 9 个报价 | 1 个需求已通过「报价→采纳」成交，1 个已关闭 |
| 经营统计 | `stat_daily` 近 30 天 | 按订单 / 支付 / 退款 / 新客口径重算 |
| 日志 | 22 条登录日志 + 519 条操作日志 | 仪表盘趋势图与模块分布有真实曲线 |

订单、会员、工单、需求的 `create_time` 已散布到近 30~45 天，仪表盘与经营统计趋势图有真实曲线。

### 用户体系与订单归属（重点）

系统里有**两套账号**，通过手机号对应，职责不同：

| 账号 | 表 | 入口 | 用途 |
| --- | --- | --- | --- |
| App 会员 | `customer` | `/app/auth/**`（手机号 + 短信验证码，未注册自动建号） | 下单主体，`orders.customer_id` 指向它 |
| 后台账号 | `sys_user` | `/admin/auth/**`（账号密码 / 验证码 / 手机号注册） | 后台登录，绑定角色（普通用户 / 会员用户等） |

- **订单永远挂在会员上**：`orders.customer_id → customer.id`，没有会员就没有订单；
  `customer.order_count` / `total_amount` 是订单有效态（已支付 / 出行中 / 已完成 / 退款中 / 已退款）的汇总。
- **注册接口 `POST /admin/auth/register`** 创建的是 `sys_user` 账号（`username = 手机号`），
  默认绑定「普通用户」；与 App 会员靠**同一手机号**对应。演示数据里两套账号手机号一一匹配，
  因此「后台用户 → App 会员 → 订单」在库里能对上。
- 演示账号 15 个，密码统一 `Abcd1234`，手机号 `13912340002` ~ `13912340016`：
  `13912340002` ~ `13912340006` 绑定「会员用户」，其余绑定「普通用户」；
  它们同时也是 `customer` 里的会员，前 12 位各带 1 笔订单（共 12 笔）。
- 会员手机号为密文，`phone_index` 为 HMAC 盲索引（等值查询用）。**不要手工改这两列**，
  新增会员走 `POST /admin/customer/customer` 或 App 验证码登录自动建号。

清空演示数据：按 `cs_message → cs_ticket → custom_quote → custom_requirement → stat_daily →
refund_record → payment_record → order_status_log → order_traveler → orders → coupon →
coupon_template → customer → route_tag_rel → route_image → route_itinerary → route_departure →
route → route_category → route_tag` 顺序 DELETE 并重置自增即可（无物理外键，顺序仅为可读性）；
演示后台账号按 `DELETE FROM sys_user_role WHERE user_id >= 5;` 后
`DELETE FROM sys_user WHERE id >= 5;` 清理。

## 数据边界

- 所有表在同一个库，本地事务可以覆盖多个业务域。
- 不建物理外键，关系由应用层维护。
- 跨业务域读写直接走本地 Service 方法，不再有 OpenFeign 的强制边界。
- 后续需要异步解耦时可重新启用 RocketMQ（当前已下线：`routego.mq.enabled=false`，dev/test 不配置 name-server，恢复方式见 `RouteGo/README.md`）。
- 图片等文件默认存 MinIO（`routego.storage.type=minio`，bucket `routego`），`sys_file.storage_type=3` 标记 MinIO、1 标记本地。
