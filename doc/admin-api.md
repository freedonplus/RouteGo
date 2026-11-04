# RouteGo Admin API 文档

- Base URL：`http://localhost:8080/api`
- 所有业务接口以 `/api/admin/**` 开头，编码统一 UTF-8。
- 在线文档：`/swagger-ui.html`，OpenAPI JSON：`/v3/api-docs`。

## 1. 通用约定

### 1.1 认证

- 登录后服务端返回 Sa-Token，并在响应头写入名为 `Authorization` 的 HttpOnly Cookie。
- App / 第三方调用使用请求头：`Authorization: Bearer <tokenValue>`。
- `tokenValue` 是**不含 Bearer 前缀**的原始 Token。
- 除验证码、登录外，所有接口都需要登录；权限不足返回 403。

### 1.2 统一响应

```json
{
  "code": 200,
  "msg": "success",
  "data": {},
  "timestamp": 1762128000000
}
```

`code` 与 HTTP 状态码一致：400 参数错误、401 未登录、403 无权限、404 不存在、409 状态冲突、429 频率限制、500 服务器错误。

### 1.3 分页

请求参数：`pageNum`（默认 1）、`pageSize`（默认 10，最大 200）。
分页响应：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [],
    "total": 0,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 0
  }
}
```

### 1.4 脱敏

- 手机号：`138****8000`
- 邮箱：`a***@example.com`
- 编辑用户时，`phone`/`email` 传 null、空串或含 `*` 的值表示不修改。

### 1.5 乐观锁

返回对象带 `version` 的接口，编辑时必须回传该 `version`；冲突返回 409。

---

## 2. 认证 `/admin/auth`

### GET `/admin/auth/captcha`

获取图形验证码，公开接口。

响应 `data`：

```json
{
  "uuid": "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed",
  "image": "iVBORw0KGgoAAAANSUhEUgAA..."
}
```

`image` 为 PNG 的 Base64（不含 `data:image/png;base64,` 前缀），2 分钟有效且只能使用一次。

### POST `/admin/auth/login`

公开接口。校验顺序：验证码 → 失败锁定 → 账号密码 → 账号状态。

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 登录账号，自动去首尾空白 |
| password | string | 是 | 明文密码，服务端 BCrypt 校验 |
| captchaUuid | string | 是 | 验证码 uuid |
| captchaCode | string | 是 | 验证码字符，不区分大小写 |

成功响应 `data`：

```json
{
  "tokenName": "Authorization",
  "tokenValue": "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed",
  "userId": 1,
  "username": "admin",
  "nickname": "管理员"
}
```

失败：400 验证码错误、401 账号或密码错误、403 账号禁用、429 失败次数过多（5 次锁定 10 分钟）。

### POST `/admin/auth/logout`

需要登录。注销当前会话并删除 Redis 中的 Token。

### GET `/admin/auth/me`

需要登录。返回当前用户：

```json
{
  "userId": 1,
  "username": "admin",
  "nickname": "管理员",
  "roles": ["super_admin"],
  "permissions": ["*"]
}
```

### PUT `/admin/auth/password`

需要登录。修改本人密码。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| oldPassword | string | 是 | 旧密码 |
| newPassword | string | 是 | 8–20 位，至少含字母和数字 |

成功后全部会话失效。

---

## 3. 系统用户 `/admin/system/user`

权限码：`system:user:list/add/edit/delete/status/reset-password/assign-role`。

### GET `/admin/system/user/page`

查询参数：`username`、`nickname`、`phone`、`status`(0/1)、`roleId`、`createTimeStart`、`createTimeEnd`、`pageNum`、`pageSize`。

### GET `/admin/system/user/{id}`

返回用户详情与 `roleIds`、`version`。

### POST `/admin/system/user`

```json
{
  "username": "ops_user",
  "password": "Passw0rd!",
  "nickname": "运营小王",
  "avatar": "https://...",
  "phone": "13812348000",
  "email": "ops@example.com",
  "remark": "",
  "roleIds": [2]
}
```

规则：账号 3–50 位字母/数字/下划线且全局唯一（含已删除）、密码强度校验、至少一个启用角色。

### PUT `/admin/system/user/{id}`

```json
{
  "version": 0,
  "nickname": "运营小王",
  "avatar": "https://...",
  "phone": "13812348000",
  "email": "ops@example.com",
  "remark": ""
}
```

账号不可修改；掩码值不覆盖。

### PUT `/admin/system/user/{id}/status?status=0`

启停用户。不能停用自己；`admin`/`super_admin` 账号受保护；禁用后立即踢下线。

### PUT `/admin/system/user/{id}/password`

```json
{ "newPassword": "NewPass123" }
```

重置后踢下线。

### GET `/admin/system/user/{id}/roles`

返回角色 ID 列表。

### PUT `/admin/system/user/{id}/roles`

```json
{ "roleIds": [2, 3] }
```

全量替换；至少一个角色；变更后清缓存并踢下线。

### DELETE `/admin/system/user/{id}`

逻辑删除，同时清理用户角色。不能删除自己或超级管理员。

---

## 4. 角色 `/admin/system/role`

权限码：`system:role:list/add/edit/delete/status/assign-menu/assign-permission`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/page` | 分页，参数 `roleName/roleCode/status` |
| GET | `/{id}` | 详情，含 `menuIds`、`permissionIds` |
| POST | `/` | 新增，`roleName`、`roleCode`、`sort`、`status`、`remark` |
| PUT | `/{id}` | 编辑名称/排序/备注，需 `version`；编码不可改 |
| DELETE | `/{id}` | 内置角色或角色下有用户时禁止 |
| PUT | `/{id}/status?status=0` | 停用后踢下线该角色用户 |
| GET | `/{id}/menus` | 已分配菜单 ID |
| PUT | `/{id}/menus` | `{ "ids": [101,102] }` 全量替换 |
| GET | `/{id}/permissions` | 已分配权限 ID |
| PUT | `/{id}/permissions` | `{ "ids": [1,2] }`；只能授予操作人已持有的权限 |
| GET | `/{id}/users` | 角色下用户分页 |

新增请求：

```json
{ "roleName": "运营", "roleCode": "ops", "sort": 10, "status": 1, "remark": "" }
```

---

## 5. 菜单 `/admin/system/menu`

权限码：`system:menu:list/add/edit/delete/status`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/tree` | 全量菜单树 |
| GET | `/user-tree` | 当前登录用户可见菜单树 |
| GET | `/{id}` | 详情 |
| POST | `/` | 新增 |
| PUT | `/{id}` | 编辑，需 `version`；不能把父级设为自己或后代 |
| DELETE | `/{id}` | 有子菜单或角色引用时禁止 |
| PUT | `/{id}/status?status=1` | 启停 |
| PUT | `/{id}/visible?visible=1` | 显示/隐藏 |

新增/编辑请求：

```json
{
  "parentId": 0,
  "menuName": "用户管理",
  "menuType": "C",
  "path": "/system/user",
  "component": "system/user/index",
  "perms": "system:user:list",
  "icon": "User",
  "sort": 1,
  "visible": 1,
  "status": 1,
  "version": 0
}
```

`menuType`：`M` 目录、`C` 菜单、`F` 按钮。

---

## 6. 权限点 `/admin/system/permission`

权限码：`system:permission:list/add/edit/delete/status`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/page` | 参数 `permissionName/permissionCode/module/status` |
| GET | `/grouped` | 按 `module` 分组的全部权限点 |
| GET | `/{id}` | 详情 |
| POST | `/` | 新增，`permissionCode` 创建后不可改 |
| PUT | `/{id}` | 编辑名称/模块/说明 |
| DELETE | `/{id}` | 被角色引用时禁止 |
| PUT | `/{id}/status?status=1` | 启停 |

新增请求：

```json
{
  "permissionName": "查看用户",
  "permissionCode": "system:user:list",
  "module": "system",
  "description": "",
  "status": 1
}
```

---

## 7. 字典 `/admin/system/dict`

权限码：`system:dict:list/add/edit/delete`。

### 字典类型 `/admin/system/dict/type`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/page` | 参数 `dictName/dictType/status` |
| GET | `/{id}` | 详情 |
| POST | `/` | 新增：`dictName`、`dictType`、`status`、`remark` |
| PUT | `/{id}` | 编辑；`dictType` 不可改 |
| DELETE | `/{id}` | 类型下有字典数据时禁止 |
| PUT | `/{id}/status?status=1` | 启停 |

### 字典数据 `/admin/system/dict/data`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/page` | 参数 `dictType/dictLabel/status` |
| GET | `/list/{dictType}` | 按类型返回启用的数据（登录即可） |
| GET | `/{id}` | 详情 |
| POST | `/` | 新增 |
| PUT | `/{id}` | 编辑 |
| DELETE | `/{id}` | 删除 |
| PUT | `/{id}/status?status=1` | 启停 |
| PUT | `/{id}/default` | 设为默认项，同类型其他默认项自动取消 |

新增请求：

```json
{
  "dictType": "sys_user_status",
  "dictLabel": "启用",
  "dictValue": "1",
  "sort": 0,
  "cssClass": "",
  "isDefault": 1,
  "status": 1,
  "remark": ""
}
```

修改字典后，`list/{dictType}` 的 Redis 缓存立即失效。

---

## 8. 系统参数 `/admin/system/config`

权限码：`system:config:list/add/edit/delete`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/page` | 参数 `configName/configKey` |
| GET | `/key/{configKey}` | 按 key 查询 |
| GET | `/{id}` | 详情 |
| POST | `/` | 新增：`configName`、`configKey`、`configValue`、`remark` |
| PUT | `/{id}` | 编辑名称/值/备注；key 不可改 |
| DELETE | `/{id}` | 删除 |
| PUT | `/batch` | 批量更新 |

批量更新请求：

```json
{ "items": [ { "configKey": "site.name", "configValue": "RouteGo" } ] }
```

---

## 9. 公告 `/admin/system/notice`

权限码：`system:notice:list/add/edit/delete`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/page` | 参数 `title/type/status` |
| GET | `/published` | 已发布公告（登录即可） |
| GET | `/{id}` | 详情 |
| POST | `/` | 新增：`title`、`content`、`type`、`status`、`isTop` |
| PUT | `/{id}` | 编辑 |
| DELETE | `/{id}` | 删除 |
| PUT | `/{id}/publish` | 发布，写 `publishTime` |
| PUT | `/{id}/unpublish` | 下架 |
| PUT | `/{id}/top?top=true` | 置顶/取消置顶 |

---

## 10. 文件 `/admin/system/file`

权限码：`system:file:upload/list/delete`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/upload` | `multipart/form-data`，字段名 `file`；白名单 jpg/jpeg/png/webp/pdf，最大 20MB |
| GET | `/page` | 参数 `originalName/fileType` |
| GET | `/{id}` | 详情 |
| GET | `/{id}/download` | 鉴权下载，需 `system:file:list` |
| DELETE | `/{id}` | 逻辑删除记录并尽力删除物理文件 |

上传响应：

```json
{
  "id": 7,
  "fileName": "a1b2c3.png",
  "originalName": "照片.png",
  "filePath": "2026/11/04/a1b2c3.png",
  "fileUrl": "/admin/system/file/7/download",
  "fileType": "png",
  "fileSize": 1024,
  "storageType": 1
}
```

---

## 11. 日志 `/admin/system`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/oper-log/page` | `system:operlog:list` | 参数 `title/operName/status/startTime/endTime` |
| GET | `/oper-log/{id}` | `system:operlog:list` | 详情 |
| GET | `/login-log/page` | `system:loginlog:list` | 参数 `username/ip/status/startTime/endTime` |

日志只读；操作日志由 `@OperLog` 切面异步写入，参数与结果已脱敏、截断。

---

## 12. 第三方绑定 `/admin/system/user-oauth`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/user/{userId}` | `system:user:list` | 用户绑定列表 |
| POST | `/` | `system:user:edit` | 绑定 |
| DELETE | `/{id}` | `system:user:edit` | 解绑 |

绑定请求：

```json
{
  "userId": 1,
  "provider": "wechat",
  "openid": "oX...",
  "unionid": "uX...",
  "nickname": "微信昵称",
  "avatar": "https://..."
}
```

`provider` 仅支持 `wechat`、`qq`、`apple`、`dingtalk`；`(provider, openid)` 唯一；受保护账号仅受保护操作人可绑定/解绑。

---

## 13. 缓存 Key 一览

| Key 模板 | TTL | 说明 |
| --- | --- | --- |
| `admin:Auth:captcha:{uuid}` | 2 分钟 | 验证码 |
| `admin:Auth:loginFail:{username}` | 10 分钟 | 登录失败计数 |
| `admin:Auth:loginLock:{username}` | 10 分钟 | 登录锁定 |
| `admin:SysUser:getById:{id}` | 10 分钟 | 用户详情 |
| `admin:SysUser:getPermissions:{id}` | 30 分钟 | 用户权限码 |
| `admin:SysUser:getRoles:{id}` | 30 分钟 | 用户角色码 |
| `admin:SysMenu:getUserTree:{id}` | 10 分钟 | 用户菜单树 |
| `admin:SysRole:getById:{id}` | 10 分钟 | 角色详情 |
| `admin:SysRole:getMenus:{id}` | 10 分钟 | 角色菜单 |
| `admin:SysRole:getPermissions:{id}` | 10 分钟 | 角色权限 |
| `admin:SysMenu:getById:{id}` | 10 分钟 | 菜单详情 |
| `admin:SysMenu:tree` | 10 分钟 | 全量菜单树 |
| `admin:SysPermission:getById:{id}` | 10 分钟 | 权限点详情 |
| `admin:SysPermission:listAll` | 10 分钟 | 全部权限点 |
| `admin:SysDictType:getByType:{type}` | 1 小时 | 字典类型 |
| `admin:SysDictData:listByType:{type}` | 1 小时 | 字典数据 |
| `admin:SysConfig:getByKey:{key}` | 1 小时 | 参数值 |
| `admin:SysNotice:listPublished` | 5 分钟 | 已发布公告 |
| `admin:SysUserOauth:listByUserId:{id}` | 10 分钟 | 第三方绑定 |
| `admin:SysFile:getById:{id}` | 10 分钟 | 文件元数据 |

---

## 14. 仪表盘与列表扩展

### GET `/admin/dashboard/overview`

登录即可访问，无需 `system:operlog:list` / `system:loginlog:list` 权限。

响应 `data`：

```json
{
  "kpis": [
    { "key": "logins", "label": "今日登录", "value": 12, "delta": 20.0, "unit": "次", "hint": "较昨日", "invert": false }
  ],
  "recentOperations": [
    { "id": 1, "operTime": "2026-11-04T10:42:00", "operName": "admin", "title": "编辑用户", "method": "PUT /admin/system/user/7", "status": 1, "costTime": 42 }
  ],
  "loginFailures": [
    { "id": 1, "username": "root", "ip": "10.0.0.1", "reason": "账号或密码错误", "time": "2026-11-04T10:05:00", "count": 1 }
  ],
  "systemStatus": [
    { "key": "database", "label": "数据库", "value": "正常", "status": "success", "detail": "连接正常" }
  ]
}
```

列表 VO 扩展（已实现）：

- `SysUserVO.roleNames`：用户角色名称列表。
- `SysRoleVO.userCount`：角色下用户数。
- `MenuNodeVO` / `SysMenuVO`：`childCount`、`roleCount`。
- `SysPermissionVO.roleCount`：引用该权限点的角色数。
