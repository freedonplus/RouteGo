# RouteGo-admin 业务模块设计（第一批）

日期：2026-11-04
状态：待确认

## 1. 目标与背景

当前仓库是模块化单体，`RouteGo-admin` 下 15 张系统表的 Entity/Mapper/Service/Controller 均为代码生成器空壳。本批目标：按 `doc/方案书.md` 的第 4.2、4.9 节、第 8 章、附录 C.1/C.8、附录 E.1，把 admin 模块做成可端到端使用的后台系统，同时落地用户要求的缓存规范、脱敏和缓存问题治理。

### 1.1 本期范围

- 认证：图形验证码、登录、登出、当前用户、修改本人密码
- 系统用户：分页、详情、新增、编辑、启停、重置密码、分配角色、删除
- 角色：分页、详情、新增、编辑、启停、删除、分配菜单、分配权限、查看角色下用户
- 菜单：树形列表、详情、新增、编辑、删除、启停、显隐、当前用户菜单树
- 权限点：分页、详情、新增、编辑、启停、删除、按模块分组
- 字典类型 / 字典数据：CRUD、启停、默认项、按类型查询
- 系统参数：分页、按 key 查询、新增、编辑、删除、批量更新
- 公告：分页、详情、新增、编辑、删除、发布/下架、置顶
- 文件：上传、分页、详情、下载、删除（本地存储）
- 操作日志 / 登录日志：只读分页、详情
- 第三方登录绑定：按用户查询、绑定、解绑
- 关联表（user_role / role_menu / role_permission）：只通过上面的"分配"接口维护，不单独暴露

### 1.2 非目标

- catalog / trade / customer / operations 四个模块（后续批次）
- 字段加密、盲索引（用户明确"先不要做加密"）
- 布隆过滤器（用户明确不做，用空值缓存）
- 微信小程序、Vue 前端、网关、Feign、Seata
- 第三方登录实际登录流程（只保留绑定关系维护）
- 公告有效期（`sys_notice` 表没有过期时间字段，本期只做发布/下架/置顶）
- 日志导出、Excel 导出（下一批）
- 敏感系统参数加密（H04 的加密部分随"不做加密"一起延期）

## 2. 现有约束

- Spring Boot 3.2.5 / Java 17 / MyBatis-Plus 3.5.7 / Sa-Token 1.44 / Redisson / Redis
- 统一响应 `com.rg.common.util.Result<T>`，分页 `PageQuery` / `PageResult`
- 逻辑删除、分页、乐观锁拦截器已在 common 配置；但实体 `version` 字段缺 `@Version`，需要补
- 实体由生成器产出，无 Lombok；新 DTO/VO 可用 Lombok（模块已引入）
- 权限数据源 `SaTokenStpInterface` 已存在，admin/super_admin 角色返回 `*`
- 现有 Controller 路径为 `/admin/sysUser` 等，本期统一改到文档规范 `/admin/system/user` 等
- 登录当前无验证码；`AuthController` 的 springdoc 注解已按现状校准，本期需要同步更新

## 3. 缓存设计（核心）

### 3.1 Key 规范

用户指定格式：`[模块]:[表/控制器]:[方法]:[参数...]`。

- 统一小写模块名，实体名用大驼峰（和表/控制器对应），方法名小写驼峰
- 所有 key 集中在 `com.rg.common.constant.CacheKeyConstants`，内部按模块用静态内部类分类
- 参数用 `%s` 占位，通过 `String.format` 生成
- 通用锁 key 也集中在同一类中

本期 admin 的 key 清单：

| 常量 | 模板 | TTL | 用途 |
| --- | --- | --- | --- |
| `Admin.CAPTCHA` | `admin:Auth:captcha:%s` | 2 分钟 | 图形验证码答案 |
| `Admin.LOGIN_FAIL` | `admin:Auth:loginFail:%s` | 10 分钟 | 登录失败计数 |
| `Admin.LOGIN_LOCK` | `admin:Auth:loginLock:%s` | 10 分钟 | 登录锁定标记 |
| `Admin.USER_BY_ID` | `admin:SysUser:getById:%s` | 10 分钟 | 用户详情 |
| `Admin.USER_PERMISSIONS` | `admin:SysUser:getPermissions:%s` | 30 分钟 | 用户权限码 |
| `Admin.USER_ROLES` | `admin:SysUser:getRoles:%s` | 30 分钟 | 用户角色码 |
| `Admin.USER_MENU_TREE` | `admin:SysMenu:getUserTree:%s` | 10 分钟 | 用户可见菜单树 |
| `Admin.ROLE_BY_ID` | `admin:SysRole:getById:%s` | 10 分钟 | 角色详情 |
| `Admin.ROLE_MENUS` | `admin:SysRole:getMenus:%s` | 10 分钟 | 角色菜单 ID |
| `Admin.ROLE_PERMISSIONS` | `admin:SysRole:getPermissions:%s` | 10 分钟 | 角色权限 ID |
| `Admin.MENU_BY_ID` | `admin:SysMenu:getById:%s` | 10 分钟 | 菜单详情 |
| `Admin.MENU_TREE` | `admin:SysMenu:tree` | 10 分钟 | 全量菜单树 |
| `Admin.PERMISSION_BY_ID` | `admin:SysPermission:getById:%s` | 10 分钟 | 权限点详情 |
| `Admin.PERMISSION_ALL` | `admin:SysPermission:listAll` | 10 分钟 | 全部权限点 |
| `Admin.DICT_TYPE_BY_TYPE` | `admin:SysDictType:getByType:%s` | 1 小时 | 字典类型 |
| `Admin.DICT_DATA_BY_TYPE` | `admin:SysDictData:listByType:%s` | 1 小时 | 某类型下字典数据 |
| `Admin.CONFIG_BY_KEY` | `admin:SysConfig:getByKey:%s` | 1 小时 | 参数值 |
| `Admin.NOTICE_PUBLISHED` | `admin:SysNotice:listPublished` | 5 分钟 | 已发布公告 |
| `Admin.OAUTH_BY_USER` | `admin:SysUserOauth:listByUserId:%s` | 10 分钟 | 用户绑定列表 |
| `Admin.FILE_BY_ID` | `admin:SysFile:getById:%s` | 10 分钟 | 文件元数据 |
| `Lock.CACHE_REBUILD` | `lock:cache:rebuild:%s` | — | 缓存重建互斥锁 |

分页列表、日志、导出类查询不缓存。

`USER_BY_ID` 缓存专用对象 `SysUserCache`（明文手机号/邮箱、不含密码、无 `@Sensitive`），VO 由 Service 从缓存转换后再交给 Jackson 脱敏。密码校验始终按 `username` 直接查库，不读缓存；改密按 ID 查库，确保拿得到密码哈希。

### 3.2 通用缓存模板

在 `com.rg.common.cache` 新增：

```java
public interface CacheHelper {
    <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader);
    void evict(String... keys);
    void evictAfterCommit(String... keys);
}
```

`RedisCacheHelper` 实现要求：

1. **穿透**：loader 返回 `null` 时写入 `CacheNullValue.INSTANCE`，TTL 固定 60 秒；读取到该标记直接返回 `null`，不回源。
2. **击穿**：未命中时用 Redisson `tryLock(3s, 10s)` 抢 `lock:cache:rebuild:{key}`；拿到锁后二次检查缓存再回源；抢锁失败短暂等待后重读缓存，最多 500ms，仍无则回源，保证可用性。
3. **雪崩**：实际 TTL = 配置 TTL × `[0.9, 1.1)` 随机抖动。
4. **一致性**：写操作先更新 DB，事务提交后通过 `evictAfterCommit` 删除相关 key；删除失败只记录日志，不回滚业务。
5. 所有缓存必须设置 TTL，禁止永久 key。

### 3.3 失效矩阵

| 变更 | 需要删除的 key |
| --- | --- |
| 用户编辑/删除/启停 | `USER_BY_ID:{id}` |
| 用户分配角色 | `USER_BY_ID`、`USER_PERMISSIONS`、`USER_ROLES`、`USER_MENU_TREE`，并踢下线 |
| 用户重置密码/改密 | `USER_BY_ID`，并踢下线 |
| 角色编辑 | `ROLE_BY_ID:{id}`；若改了状态或名称，另按角色反查用户，删其 `USER_ROLES`、`USER_PERMISSIONS`、`USER_MENU_TREE` |
| 角色启停/删除 | 同上；并踢下线该角色下所有用户 |
| 角色分配菜单 | `ROLE_MENUS:{id}`、该角色下用户的 `USER_MENU_TREE`（不踢下线，下次请求重新加载） |
| 角色分配权限 | `ROLE_PERMISSIONS:{id}`、该角色下用户的 `USER_PERMISSIONS`（不踢下线） |
| 菜单新增/编辑/删除/启停/显隐 | `MENU_BY_ID`、`MENU_TREE`、该菜单关联角色的 `ROLE_MENUS`、相关用户 `USER_MENU_TREE` |
| 权限点新增/编辑/删除/启停 | `PERMISSION_BY_ID`、`PERMISSION_ALL`、`USER_PERMISSIONS`（按角色反查用户） |
| 字典类型变更 | `DICT_TYPE_BY_TYPE:{type}` |
| 字典数据变更 | `DICT_DATA_BY_TYPE:{type}` |
| 参数变更 | `CONFIG_BY_KEY:{key}` |
| 公告发布/下架/编辑/删除 | `NOTICE_PUBLISHED` |
| OAuth 绑定/解绑 | `OAUTH_BY_USER:{userId}` |
| 文件删除 | `FILE_BY_ID:{id}` |

`CacheKeyConstants` 只放模板，具体 key 由 `String.format` 生成；失效矩阵中的 `:*` 通过已知用户/角色 ID 精确删除，不做 Redis `SCAN`/`KEYS`。

### 3.4 权限缓存接入

`SaTokenStpInterface.getPermissionList` / `getRoleList` 改为先读 `USER_PERMISSIONS` / `USER_ROLES` 缓存，未命中回源两表查询，再写缓存。这样每个请求的鉴权不再直查数据库。

## 4. 脱敏设计

### 4.1 组件

`com.rg.common.sensitive`：

- `@Sensitive(SensitiveType type)`：字段注解，元注解 `@JacksonAnnotationsInside` + `@JsonSerialize(using = SensitiveSerializer.class)`
- `SensitiveType`：`PHONE`、`EMAIL`、`NAME`、`ID_CARD`、`BANK_CARD`
- `SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer`：从字段注解读取类型，输出掩码

掩码规则：

| 类型 | 规则 | 示例 |
| --- | --- | --- |
| PHONE | 前 3 + `****` + 后 4 | `138****8000` |
| EMAIL | 首字符 + `***` + `@域名` | `a***@example.com` |
| NAME | 首字符 + 每个剩余字符一个 `*` | `张*`、`欧阳**` |
| ID_CARD | 前 3 + `*` + 后 4 | `110***********1234` |
| BANK_CARD | 只保留后 4 位 | `**** **** **** 1234` |

null / 空串 / 长度不足按原样返回。

### 4.2 应用范围

- `SysUserVO.phone` 用 `@Sensitive(PHONE)`；`email` 用 `@Sensitive(EMAIL)`
- 密码字段永不进入 VO；`SysUser` 实体的 `password` 增加 `@JsonIgnore` 作为兜底
- 手机号明文入库（不做加密、不写 `phone_index`），按 key 查询直接查 `phone`
- 日志切面对 `password`、`phone`、`idCard` 等键名做脱敏后再落库
- 编辑回写契约：Update DTO 中 `phone`/`email` 为 null、空串或包含 `*`（即前端把掩码原样提交）时视为"不修改"；只有提交不含 `*` 的明文才覆盖。本期不提供查看明文的单独接口，需要明文编辑时由管理员直接输入完整值

## 5. 认证与验证码

### 5.1 验证码（自建 4 位字符 + 干扰线）

- `GET /admin/auth/captcha`（白名单），返回 `{ "uuid": "...", "image": "<base64 png>" }`
- 字符集 `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`（去掉 0/O/1/I），4 位，不区分大小写
- Java2D 生成 120×40 PNG：随机旋转、5 条贝塞尔/直线干扰线、30 个噪点、渐变背景
- 答案写 `admin:Auth:captcha:{uuid}`，TTL 2 分钟
- 登录校验使用 Redis `GETDEL` 原子消费：无论成功失败都删除
- 登录 DTO 增加 `captchaUuid`、`captchaCode`，校验顺序：验证码 → 失败锁定 → 账号密码 → 账号状态

实现放在 `com.rg.common.captcha.CaptchaGenerator`（纯绘图，可复用），admin 的 `CaptchaService` 负责存取 Redis。

### 5.2 登录

- 失败计数 `admin:Auth:loginFail:{username}`，TTL 10 分钟；达到 5 次写 `admin:Auth:loginLock:{username}`，TTL 10 分钟
- 锁定期内直接返回 429 "登录失败次数过多，请 10 分钟后再试"
- 账号不存在时执行一次固定的 dummy BCrypt 比对，拉平耗时
- 成功/失败都异步写 `sys_login_log`；成功后清零失败计数
- 成功后更新 `last_login_time` / `last_login_ip`
- 客户端 IP：默认 `getRemoteAddr()`；仅当 `routego.security.trusted-proxies` 配置了代理地址且直接对端命中时，才取 `X-Forwarded-For` 最后一个可信跳点
- 登录、登出、当前用户路径保持现状：`/admin/auth/login`、`/logout`、`/me`

### 5.3 改密与踢下线

- `PUT /admin/auth/password`：校验旧密码、新密码强度，更新后 `StpUtil.logout(userId)` 使全部会话失效
- 禁用用户、重置密码、分配角色、删除用户时同样踢下线
- 密码强度：8–20 位，至少包含字母和数字（正则 `^(?=.*[A-Za-z])(?=.*\d)[\S]{8,20}$`）

## 6. 接口设计

统一前缀 `/api`（context-path）→ 控制器路径：

### 6.1 认证 `/admin/auth`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/captcha` | 获取验证码 | 公开 |
| POST | `/login` | 登录 | 公开 |
| POST | `/logout` | 登出 | 登录 |
| GET | `/me` | 当前用户 | 登录 |
| PUT | `/password` | 修改本人密码 | 登录 |

### 6.2 系统用户 `/admin/system/user`

| 方法 | 路径 | 权限码 |
| --- | --- | --- |
| GET | `/page` | `system:user:list` |
| GET | `/{id}` | `system:user:list` |
| POST | `/` | `system:user:add` |
| PUT | `/{id}` | `system:user:edit` |
| DELETE | `/{id}` | `system:user:delete` |
| PUT | `/{id}/status` | `system:user:status` |
| PUT | `/{id}/password` | `system:user:reset-password` |
| GET | `/{id}/roles` | `system:user:list` |
| PUT | `/{id}/roles` | `system:user:assign-role` |

查询条件：`username`、`nickname`、`phone`、`status`、`roleId`、`createTimeStart/End`。

### 6.3 角色 `/admin/system/role`

| 方法 | 路径 | 权限码 |
| --- | --- | --- |
| GET | `/page` | `system:role:list` |
| GET | `/{id}` | `system:role:list` |
| POST | `/` | `system:role:add` |
| PUT | `/{id}` | `system:role:edit` |
| DELETE | `/{id}` | `system:role:delete` |
| PUT | `/{id}/status` | `system:role:status` |
| GET | `/{id}/menus` | `system:role:list` |
| PUT | `/{id}/menus` | `system:role:assign-menu` |
| GET | `/{id}/permissions` | `system:role:list` |
| PUT | `/{id}/permissions` | `system:role:assign-permission` |
| GET | `/{id}/users` | `system:role:list` |

### 6.4 菜单 `/admin/system/menu`

| 方法 | 路径 | 权限码 |
| --- | --- | --- |
| GET | `/tree` | `system:menu:list` |
| GET | `/user-tree` | 登录（返回当前用户可见菜单） |
| GET | `/{id}` | `system:menu:list` |
| POST | `/` | `system:menu:add` |
| PUT | `/{id}` | `system:menu:edit` |
| DELETE | `/{id}` | `system:menu:delete` |
| PUT | `/{id}/status` | `system:menu:status` |
| PUT | `/{id}/visible` | `system:menu:edit` |

### 6.5 权限点 `/admin/system/permission`

| 方法 | 路径 | 权限码 |
| --- | --- | --- |
| GET | `/page` | `system:permission:list` |
| GET | `/grouped` | `system:permission:list` |
| GET | `/{id}` | `system:permission:list` |
| POST | `/` | `system:permission:add` |
| PUT | `/{id}` | `system:permission:edit` |
| DELETE | `/{id}` | `system:permission:delete` |
| PUT | `/{id}/status` | `system:permission:status` |

### 6.6 字典 `/admin/system/dict`

| 方法 | 路径 | 权限码 |
| --- | --- | --- |
| GET | `/type/page` | `system:dict:list` |
| GET | `/type/{id}` | `system:dict:list` |
| POST | `/type` | `system:dict:add` |
| PUT | `/type/{id}` | `system:dict:edit` |
| DELETE | `/type/{id}` | `system:dict:delete` |
| PUT | `/type/{id}/status` | `system:dict:edit` |
| GET | `/data/page` | `system:dict:list` |
| GET | `/data/list/{dictType}` | 登录（下拉框使用） |
| GET | `/data/{id}` | `system:dict:list` |
| POST | `/data` | `system:dict:add` |
| PUT | `/data/{id}` | `system:dict:edit` |
| DELETE | `/data/{id}` | `system:dict:delete` |
| PUT | `/data/{id}/status` | `system:dict:edit` |
| PUT | `/data/{id}/default` | `system:dict:edit` |

### 6.7 参数 `/admin/system/config`

| 方法 | 路径 | 权限码 |
| --- | --- | --- |
| GET | `/page` | `system:config:list` |
| GET | `/key/{configKey}` | `system:config:list` |
| GET | `/{id}` | `system:config:list` |
| POST | `/` | `system:config:add` |
| PUT | `/{id}` | `system:config:edit` |
| DELETE | `/{id}` | `system:config:delete` |
| PUT | `/batch` | `system:config:edit` |

### 6.8 公告 `/admin/system/notice`

| 方法 | 路径 | 权限码 |
| --- | --- | --- |
| GET | `/page` | `system:notice:list` |
| GET | `/published` | 登录 |
| GET | `/{id}` | `system:notice:list` |
| POST | `/` | `system:notice:add` |
| PUT | `/{id}` | `system:notice:edit` |
| DELETE | `/{id}` | `system:notice:delete` |
| PUT | `/{id}/publish` | `system:notice:edit` |
| PUT | `/{id}/unpublish` | `system:notice:edit` |
| PUT | `/{id}/top` | `system:notice:edit` |

### 6.9 文件 `/admin/system/file`

| 方法 | 路径 | 权限码 |
| --- | --- | --- |
| POST | `/upload` | `system:file:upload` |
| GET | `/page` | `system:file:list` |
| GET | `/{id}` | `system:file:list` |
| GET | `/{id}/download` | 登录 |
| DELETE | `/{id}` | `system:file:delete` |

### 6.10 日志 `/admin/system`

| 方法 | 路径 | 权限码 |
| --- | --- | --- |
| GET | `/oper-log/page` | `system:operlog:list` |
| GET | `/oper-log/{id}` | `system:operlog:list` |
| GET | `/login-log/page` | `system:loginlog:list` |

### 6.11 第三方绑定 `/admin/system/user-oauth`

| 方法 | 路径 | 权限码 |
| --- | --- | --- |
| GET | `/user/{userId}` | `system:user:list` |
| POST | `/` | `system:user:edit` |
| DELETE | `/{id}` | `system:user:edit` |

## 7. 业务规则

### 7.0 编码与并发约定

- `username`、`role_code`、`permission_code`、`dict_type`、`config_key` 创建后**不可修改**（保护 `@SaCheckPermission` 硬编码、内置角色判断、字典/参数缓存）
- 以上编码的逻辑删除行**永久占用编码**：新增/编辑查重时连 `deleted=1` 一起查，重复返回 409 并提示"编码已被使用（可能已删除）"；不改唯一索引、不物理删除
- 所有 Entity 的 `version` 字段加 `@Version`；VO 返回 `version`，Update DTO 必填 `version`；`updateById` 影响行数为 0 时抛 409"数据已被修改，请刷新后重试"
- 登录时更新 `last_login_time`/`last_login_ip` 使用不带版本条件的定向 `UPDATE`，避免把用户版本号抬高导致后台编辑冲突

### 7.1 用户

- 用户名唯一，创建后不可修改；新增时至少分配一个角色
- 新增/重置密码必须满足密码强度；BCrypt cost 10
- 不能停用/删除当前登录账号
- 拥有 `admin` / `super_admin` 角色的账号受保护：不能停用、不能删除
- 禁用、重置密码、分配角色后调用 `StpUtil.logout(userId)` 踢下线
- 删除为逻辑删除，同事务删除 `sys_user_role`；不能删除自己或超级管理员
- 分页与详情不返回密码

### 7.2 角色

- `role_code` 唯一；内置角色 `admin`、`super_admin` 不可删除、不可停用
- 角色下存在用户时禁止删除
- 分配菜单/权限为全量替换：先删后插，同一事务；至少保留原有语义（允许空集合）
- 分配前校验菜单/权限存在；权限点必须启用
- 角色停用/删除：按失效矩阵删除该角色下用户的 `USER_ROLES`/`USER_PERMISSIONS`/`USER_MENU_TREE` 并踢下线
- 角色分配菜单/权限：只删除角色与用户级缓存，不踢下线，下一次请求重新加载

### 7.3 菜单

- `menu_type` ∈ {M, C, F}；父级必须存在；不能把父级设为自己或后代，防止环
- 有子菜单或已被角色引用时禁止删除
- 树按 `sort` 升序、`id` 升序构建
- 当前用户菜单树：超级管理员返回全部启用且可见菜单；其他用户按角色关联菜单过滤，只保留 M/C 类型

### 7.4 权限点

- `permission_code` 唯一，命名遵循 `模块:动作`
- 已被角色引用时禁止删除
- 分组查询按 `module` 聚合

### 7.5 字典

- `dict_type` 唯一；类型下存在字典数据时禁止删除类型（H02）
- 同一 `dict_type` 下 `dict_value` 唯一；类型必须存在且启用
- 设默认项时清除同类型其他默认项
- 任何变更删除对应缓存（H03）

### 7.6 参数、公告、文件、日志、OAuth

- 参数 `config_key` 唯一；按 key 查询走缓存；批量更新同事务
- 公告标题必填；发布写 `publish_time`，下架置 `status=0`；列表置顶优先、发布时间倒序
- 文件上传白名单 `jpg/jpeg/png/webp/pdf`，单文件上限 20MB，重命名 `UUID.ext`，本地目录 `routego.storage.local-path`（默认 `./uploads`）
- 上传响应返回 `{ id, fileName, originalName, size, url }`；`file_url` 存带鉴权的相对路径 `/admin/system/file/{id}/download`，不存物理路径、不签发限时 URL
- 文件下载必须登录，使用 `ResponseEntity<Resource>` 返回，不暴露静态目录；前端 `<img>` 需带 Cookie（同站部署）或先用 fetch 取 blob
- 用户头像同样存这个下载路径，不单独提供公开 URL
- 删除文件：逻辑删除记录 + 尽力删除物理文件（失败只记日志）
- 操作日志/登录日志只读；登录失败也写日志
- OAuth 绑定 `(provider, openid)` 唯一；解绑前确认用户仍有密码（`sys_user.password` 非空）

## 8. 操作日志

新增 `@OperLog(title, type)` 注解 + `OperLogAspect`：

- 注解放在 Controller 写操作方法上（新增/修改/删除/状态/分配/导出）
- 切面采集：操作人（Sa-Token 登录 ID + 用户名）、URL、HTTP 方法、IP、参数 JSON、返回值、耗时、成功/失败、异常信息
- 参数与结果截断（默认 2000 字符），对 `password`、`phone`、`idCard`、`token` 等键名脱敏
- 异步写入 `sys_oper_log`，避免阻塞主流程
- common 定义 `OperLogRecorder` SPI 接口；admin 提供 MyBatis-Plus 实现，避免 common 依赖业务实体

## 9. 代码结构

```
RouteGo-common
  constant/CacheKeyConstants.java
  cache/CacheHelper.java, RedisCacheHelper.java, CacheNullValue.java
  sensitive/Sensitive.java, SensitiveType.java, SensitiveSerializer.java
  captcha/CaptchaGenerator.java
  annotation/OperLog.java, BusinessType.java
  log/OperLogRecorder.java
  util/IpUtils.java

RouteGo-admin
  controller/AuthController.java
  controller/SysUserController.java
  controller/SysRoleController.java
  controller/SysMenuController.java
  controller/SysPermissionController.java
  controller/SysDictTypeController.java
  controller/SysDictDataController.java
  controller/SysConfigController.java
  controller/SysNoticeController.java
  controller/SysFileController.java
  controller/SysLoginLogController.java
  controller/SysOperLogController.java
  controller/SysUserOauthController.java
  dto/...（Create/Update/Query/Assign 请求）
  vo/...（不含密码，敏感字段带 @Sensitive）
  service/... + impl/...
  aspect/OperLogAspect.java（或 common）
  config/StorageProperties.java
```

DTO/VO 全部带 springdoc `@Schema`；Controller 带 `@Tag`、`@Operation`。

## 10. 数据库与实体调整

- 给所有带 `version` 字段的 admin 实体加 `@Version`（当前只是普通 `@TableField`，乐观锁不生效）
- 修正 `SysPermissionMapper` / `SysRoleMapper` 的自定义 SQL：join `sys_role` 并过滤 `r.deleted=0 AND r.status=1`，权限点过滤 `p.deleted=0 AND p.status=1`
- 新增 `sql/admin-init.sql`（可选执行）：内置角色 `admin`、`super_admin`，admin 菜单树与权限点目录；不预置用户密码，文档说明用 BCrypt 工具生成
- README 补充 admin 接口清单、缓存 key 规则、验证码与初始化步骤

## 11. 测试与验收

- 编译：`mvn -o -pl RouteGo-modules/RouteGo-admin -am -DskipTests compile`
- 单元测试（common）：
  - 脱敏序列化各类型边界（null、短串、正常）
  - `CacheHelper` 空值缓存、TTL 抖动、锁重建（用 mock Redis/Redisson）
  - 验证码生成与一次性消费
  - 菜单树构建、环检测
- 单元测试（admin）：
  - 用户创建/编辑/删除/启停/分配角色规则（Mockito）
  - 角色删除保护、内置角色保护
  - 字典默认项唯一、缓存失效调用
- 手工冒烟（需要 MySQL + Redis）：
  1. `GET /api/admin/auth/captcha` 拿 uuid + 图片
  2. 登录成功/验证码错误/密码错误/禁用账号/连续失败锁定
  3. 用户 CRUD、分配角色、禁用后旧 token 失效
  4. 角色分配菜单/权限，二次登录菜单树变化
  5. 字典修改后列表立即变化（缓存已删）
  6. 公告发布后 `/published` 可见
  7. 文件上传/下载/删除
- 验收标准：上述接口全部可用；缓存 key 均命中规范；手机号在响应中脱敏；重复失败登录被锁定；缓存为空/不存在时不会打穿数据库。

## 12. 交付顺序

1. common 基础设施：常量、CacheHelper、脱敏、验证码、OperLog SPI
2. admin 实体/ Mapper 修正、DTO/VO
3. 认证：验证码、登录锁定、登录日志、改密、踢下线
4. 用户 + 角色 + 菜单 + 权限
5. 字典 + 参数
6. 公告 + 文件
7. 日志查询 + OAuth
8. 测试、seed SQL、README 更新

## 13. 已做的决定（供确认）

- 缓存只覆盖单对象/字典类读，分页不缓存
- 权限/角色列表也进缓存，接入 `SaTokenStpInterface`
- 手机号明文存储，不写盲索引（落实"先不加密"）
- 公告无有效期字段，本期不做过期逻辑
- 日志导出、Excel 导出延后
- 菜单树、角色菜单、角色权限缓存按用户/角色精确失效，不使用 `KEYS`/`SCAN`
- 默认不预置管理员账号，避免仓库自带弱口令
- 业务编码（username/role_code/permission_code/dict_type/config_key）不可修改；逻辑删除后编码仍占用，查重包含已删除行
- `USER_BY_ID` 缓存明文 `SysUserCache`（无密码、无 `@Sensitive`），HTTP 输出才脱敏；密码相关流程一律查库
- 更新接口走 `version` 乐观锁，冲突返回 409；登录写 `last_login_*` 用定向 UPDATE 不碰版本
- 手机号/邮箱编辑时，null、空串或带 `*` 的值视为不修改，防止掩码回写
- 角色停用/删除踢下线；角色分配菜单/权限只清缓存不踢下线
- 文件 `file_url` 存鉴权下载路径，头像走同一路径
