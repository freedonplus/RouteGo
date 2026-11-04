# Admin 手工冒烟清单

前置：MySQL 已导入 `sql/routego.sql` 与 `sql/admin-init.sql`，Redis 可用，应用启动在 `8080`。

1. 验证码与登录
   - `GET /api/admin/auth/captcha` 返回 uuid 与图片
   - 错误验证码返回 400；连续 5 次错误密码后返回 429
   - 正确登录返回 token，并收到 `Authorization` Cookie
2. 当前用户与改密
   - `GET /api/admin/auth/me` 返回角色与权限码
   - `PUT /api/admin/auth/password` 后旧 token 失效
3. 用户管理
   - 新增用户（至少一个角色）、编辑（掩码手机号不覆盖）、启停、重置密码、分配角色
   - 禁用/分配角色/重置密码后，该用户旧 token 立即失效
4. 角色与授权
   - 内置角色不能删除/停用
   - 分配菜单/权限后，对应用户的 `/me` 权限码和 `/menu/user-tree` 变化
5. 菜单与权限点
   - 菜单树按 sort 排序；父级不能设为自己或后代
   - 有子菜单或角色引用时不能删除
6. 字典与参数
   - 修改字典数据后 `GET /dict/data/list/{type}` 立即变化
   - 修改参数后 `GET /config/key/{key}` 立即变化
7. 公告与文件
   - 发布后 `/notice/published` 可见；下架后消失
   - 上传 png 成功，exe 被拒绝；下载需要登录
8. 日志
   - 写操作后 `/oper-log/page` 出现脱敏记录
   - 登录成功/失败都出现在 `/login-log/page`
9. 缓存
   - 删除 Redis 中 `admin:SysUser:getById:{id}` 后再次查询能重建
   - 查询不存在的数据两次，第二次命中空值缓存（60 秒内不重复查库）
