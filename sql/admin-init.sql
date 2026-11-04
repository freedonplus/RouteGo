-- RouteGo admin 初始化脚本（可选执行，幂等）
-- 升级后请重新执行本脚本：权限码已统一为 admin:<Controller>:<method>，
-- 脚本会按 id 覆盖旧编码，并保持 sys_role_permission 的关联不变。
-- 仅创建内置角色、权限点、菜单及授权；不预置任何用户账号。
-- 需要管理员时，用 BCrypt 生成密码后手工插入：
--   INSERT INTO sys_user (username, password, nickname, status, deleted)
--   VALUES ('your_admin', '<BCrypt 密文>', '管理员', 1, 0);
--   再把该用户加入 super_admin 角色：INSERT INTO sys_user_role (user_id, role_id) ...

-- 1. 内置角色
INSERT INTO sys_role (id, role_name, role_code, sort, status, remark, version, deleted)
VALUES (1, '系统管理员', 'admin', 10, 1, '后台运营管理员', 0, 0),
       (2, '超级管理员', 'super_admin', 0, 1, '拥有全部权限', 0, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = VALUES(status);

-- 2. 权限点（与 Controller 上的 @SaCheckPermission 一一对应）
-- 格式：admin:<Controller>:<method>
INSERT INTO sys_permission (id, permission_name, permission_code, module, status, deleted)
VALUES
 (1, '仪表盘概览', 'admin:Dashboard:overview', 'admin', 1, 0),
 (2, '参数分页', 'admin:SysConfig:page', 'admin', 1, 0),
 (3, '按 key 查询参数', 'admin:SysConfig:getByKey', 'admin', 1, 0),
 (4, '参数详情', 'admin:SysConfig:detail', 'admin', 1, 0),
 (5, '新增参数', 'admin:SysConfig:create', 'admin', 1, 0),
 (6, '编辑参数', 'admin:SysConfig:update', 'admin', 1, 0),
 (7, '删除参数', 'admin:SysConfig:delete', 'admin', 1, 0),
 (8, '批量更新参数', 'admin:SysConfig:batchUpdate', 'admin', 1, 0),
 (9, '字典数据分页', 'admin:SysDictData:page', 'admin', 1, 0),
 (10, '字典数据详情', 'admin:SysDictData:detail', 'admin', 1, 0),
 (11, '新增字典数据', 'admin:SysDictData:create', 'admin', 1, 0),
 (12, '编辑字典数据', 'admin:SysDictData:update', 'admin', 1, 0),
 (13, '删除字典数据', 'admin:SysDictData:delete', 'admin', 1, 0),
 (14, '启用/禁用字典数据', 'admin:SysDictData:updateStatus', 'admin', 1, 0),
 (15, '设为默认项', 'admin:SysDictData:setDefault', 'admin', 1, 0),
 (16, '字典类型分页', 'admin:SysDictType:page', 'admin', 1, 0),
 (17, '字典类型详情', 'admin:SysDictType:detail', 'admin', 1, 0),
 (18, '新增字典类型', 'admin:SysDictType:create', 'admin', 1, 0),
 (19, '编辑字典类型', 'admin:SysDictType:update', 'admin', 1, 0),
 (20, '删除字典类型', 'admin:SysDictType:delete', 'admin', 1, 0),
 (21, '启用/禁用字典类型', 'admin:SysDictType:updateStatus', 'admin', 1, 0),
 (22, '上传文件', 'admin:SysFile:upload', 'admin', 1, 0),
 (23, '文件分页', 'admin:SysFile:page', 'admin', 1, 0),
 (24, '文件详情', 'admin:SysFile:detail', 'admin', 1, 0),
 (25, '下载文件', 'admin:SysFile:download', 'admin', 1, 0),
 (26, '删除文件', 'admin:SysFile:delete', 'admin', 1, 0),
 (27, '登录日志分页', 'admin:SysLoginLog:page', 'admin', 1, 0),
 (28, '菜单树', 'admin:SysMenu:tree', 'admin', 1, 0),
 (29, '菜单详情', 'admin:SysMenu:detail', 'admin', 1, 0),
 (30, '新增菜单', 'admin:SysMenu:create', 'admin', 1, 0),
 (31, '编辑菜单', 'admin:SysMenu:update', 'admin', 1, 0),
 (32, '删除菜单', 'admin:SysMenu:delete', 'admin', 1, 0),
 (33, '启用/禁用菜单', 'admin:SysMenu:updateStatus', 'admin', 1, 0),
 (34, '显示/隐藏菜单', 'admin:SysMenu:updateVisible', 'admin', 1, 0),
 (35, '公告分页', 'admin:SysNotice:page', 'admin', 1, 0),
 (36, '公告详情', 'admin:SysNotice:detail', 'admin', 1, 0),
 (37, '新增公告', 'admin:SysNotice:create', 'admin', 1, 0),
 (38, '编辑公告', 'admin:SysNotice:update', 'admin', 1, 0),
 (39, '删除公告', 'admin:SysNotice:delete', 'admin', 1, 0),
 (40, '发布公告', 'admin:SysNotice:publish', 'admin', 1, 0),
 (41, '下架公告', 'admin:SysNotice:unpublish', 'admin', 1, 0),
 (42, '置顶/取消置顶', 'admin:SysNotice:setTop', 'admin', 1, 0),
 (43, '操作日志分页', 'admin:SysOperLog:page', 'admin', 1, 0),
 (44, '操作日志详情', 'admin:SysOperLog:detail', 'admin', 1, 0),
 (45, '权限点分页', 'admin:SysPermission:page', 'admin', 1, 0),
 (46, '权限点分组', 'admin:SysPermission:grouped', 'admin', 1, 0),
 (47, '权限点详情', 'admin:SysPermission:detail', 'admin', 1, 0),
 (48, '新增权限点', 'admin:SysPermission:create', 'admin', 1, 0),
 (49, '编辑权限点', 'admin:SysPermission:update', 'admin', 1, 0),
 (50, '删除权限点', 'admin:SysPermission:delete', 'admin', 1, 0),
 (51, '启用/禁用权限点', 'admin:SysPermission:updateStatus', 'admin', 1, 0),
 (52, '角色分页', 'admin:SysRole:page', 'admin', 1, 0),
 (53, '角色详情', 'admin:SysRole:detail', 'admin', 1, 0),
 (54, '新增角色', 'admin:SysRole:create', 'admin', 1, 0),
 (55, '编辑角色', 'admin:SysRole:update', 'admin', 1, 0),
 (56, '启用/禁用角色', 'admin:SysRole:updateStatus', 'admin', 1, 0),
 (57, '删除角色', 'admin:SysRole:delete', 'admin', 1, 0),
 (58, '查询角色菜单', 'admin:SysRole:listMenus', 'admin', 1, 0),
 (59, '分配角色菜单', 'admin:SysRole:assignMenus', 'admin', 1, 0),
 (60, '查询角色权限点', 'admin:SysRole:listPermissions', 'admin', 1, 0),
 (61, '分配角色权限点', 'admin:SysRole:assignPermissions', 'admin', 1, 0),
 (62, '角色下用户分页', 'admin:SysRole:pageUsers', 'admin', 1, 0),
 (63, '用户分页', 'admin:SysUser:page', 'admin', 1, 0),
 (64, '用户详情', 'admin:SysUser:detail', 'admin', 1, 0),
 (65, '新增用户', 'admin:SysUser:create', 'admin', 1, 0),
 (66, '编辑用户', 'admin:SysUser:update', 'admin', 1, 0),
 (67, '启用/禁用用户', 'admin:SysUser:updateStatus', 'admin', 1, 0),
 (68, '重置用户密码', 'admin:SysUser:resetPassword', 'admin', 1, 0),
 (69, '查询用户角色', 'admin:SysUser:listRoles', 'admin', 1, 0),
 (70, '分配用户角色', 'admin:SysUser:assignRoles', 'admin', 1, 0),
 (71, '删除用户', 'admin:SysUser:delete', 'admin', 1, 0),
 (72, '用户绑定列表', 'admin:SysUserOauth:listByUser', 'admin', 1, 0),
 (73, '绑定第三方账号', 'admin:SysUserOauth:bind', 'admin', 1, 0),
 (74, '解绑第三方账号', 'admin:SysUserOauth:unbind', 'admin', 1, 0)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), status = 1;

-- 3. 菜单（目录 + 菜单）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, version, deleted)
VALUES
 (100, 0,   '系统管理', 'M', '/system',       NULL,                  NULL,                          'Settings', 1, 1, 1, 0, 0),
 (101, 100, '用户管理', 'C', '/system/user',  'system/user/index',   'admin:SysUser:page',            'User',     1, 1, 1, 0, 0),
 (102, 100, '角色管理', 'C', '/system/role',  'system/role/index',   'admin:SysRole:page',            'Team',     2, 1, 1, 0, 0),
 (103, 100, '菜单管理', 'C', '/system/menu',  'system/menu/index',   'admin:SysMenu:tree',            'Menu',     3, 1, 1, 0, 0),
 (104, 100, '权限点',   'C', '/system/permission', 'system/permission/index', 'admin:SysPermission:page', 'Key', 4, 1, 1, 0, 0),
 (105, 100, '字典管理', 'C', '/system/dict',  'system/dict/index',   'admin:SysDictType:page',            'Book',     5, 1, 1, 0, 0),
 (106, 100, '参数配置', 'C', '/system/config','system/config/index', 'admin:SysConfig:page',          'SetUp',    6, 1, 1, 0, 0),
 (107, 100, '公告管理', 'C', '/system/notice','system/notice/index', 'admin:SysNotice:page',          'Bell',     7, 1, 1, 0, 0),
 (108, 100, '文件管理', 'C', '/system/file',  'system/file/index',   'admin:SysFile:page',            'Folder',   8, 1, 1, 0, 0),
 (109, 100, '操作日志', 'C', '/system/oper-log', 'system/operlog/index', 'admin:SysOperLog:page',     'Document', 9, 1, 1, 0, 0),
 (110, 100, '登录日志', 'C', '/system/login-log','system/loginlog/index','admin:SysLoginLog:page',    'Clock',   10, 1, 1, 0, 0)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), status = 1;

-- 4. 两个内置角色授予全部菜单与权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
