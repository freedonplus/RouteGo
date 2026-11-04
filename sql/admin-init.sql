-- RouteGo admin 初始化脚本（可选执行，幂等）
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
INSERT INTO sys_permission (id, permission_name, permission_code, module, status, deleted)
VALUES
 (1,  '用户列表',     'system:user:list',            'system', 1, 0),
 (2,  '新增用户',     'system:user:add',             'system', 1, 0),
 (3,  '编辑用户',     'system:user:edit',            'system', 1, 0),
 (4,  '删除用户',     'system:user:delete',          'system', 1, 0),
 (5,  '启停用户',     'system:user:status',          'system', 1, 0),
 (6,  '重置密码',     'system:user:reset-password',  'system', 1, 0),
 (7,  '分配角色',     'system:user:assign-role',     'system', 1, 0),
 (8,  '角色列表',     'system:role:list',            'system', 1, 0),
 (9,  '新增角色',     'system:role:add',             'system', 1, 0),
 (10, '编辑角色',     'system:role:edit',            'system', 1, 0),
 (11, '删除角色',     'system:role:delete',          'system', 1, 0),
 (12, '启停角色',     'system:role:status',          'system', 1, 0),
 (13, '分配菜单',     'system:role:assign-menu',     'system', 1, 0),
 (14, '分配权限',     'system:role:assign-permission','system', 1, 0),
 (15, '菜单列表',     'system:menu:list',            'system', 1, 0),
 (16, '新增菜单',     'system:menu:add',             'system', 1, 0),
 (17, '编辑菜单',     'system:menu:edit',            'system', 1, 0),
 (18, '删除菜单',     'system:menu:delete',          'system', 1, 0),
 (19, '启停菜单',     'system:menu:status',          'system', 1, 0),
 (20, '权限点列表',   'system:permission:list',      'system', 1, 0),
 (21, '新增权限点',   'system:permission:add',       'system', 1, 0),
 (22, '编辑权限点',   'system:permission:edit',      'system', 1, 0),
 (23, '删除权限点',   'system:permission:delete',    'system', 1, 0),
 (24, '启停权限点',   'system:permission:status',    'system', 1, 0),
 (25, '字典列表',     'system:dict:list',            'system', 1, 0),
 (26, '新增字典',     'system:dict:add',             'system', 1, 0),
 (27, '编辑字典',     'system:dict:edit',            'system', 1, 0),
 (28, '删除字典',     'system:dict:delete',          'system', 1, 0),
 (29, '参数列表',     'system:config:list',          'system', 1, 0),
 (30, '新增参数',     'system:config:add',           'system', 1, 0),
 (31, '编辑参数',     'system:config:edit',          'system', 1, 0),
 (32, '删除参数',     'system:config:delete',        'system', 1, 0),
 (33, '公告列表',     'system:notice:list',          'system', 1, 0),
 (34, '新增公告',     'system:notice:add',           'system', 1, 0),
 (35, '编辑公告',     'system:notice:edit',          'system', 1, 0),
 (36, '删除公告',     'system:notice:delete',        'system', 1, 0),
 (37, '上传文件',     'system:file:upload',          'system', 1, 0),
 (38, '文件列表',     'system:file:list',            'system', 1, 0),
 (39, '删除文件',     'system:file:delete',          'system', 1, 0),
 (40, '操作日志',     'system:operlog:list',         'system', 1, 0),
 (41, '登录日志',     'system:loginlog:list',        'system', 1, 0)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), status = 1;

-- 3. 菜单（目录 + 菜单）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, version, deleted)
VALUES
 (100, 0,   '系统管理', 'M', '/system',       NULL,                  NULL,                          'Settings', 1, 1, 1, 0, 0),
 (101, 100, '用户管理', 'C', '/system/user',  'system/user/index',   'system:user:list',            'User',     1, 1, 1, 0, 0),
 (102, 100, '角色管理', 'C', '/system/role',  'system/role/index',   'system:role:list',            'Team',     2, 1, 1, 0, 0),
 (103, 100, '菜单管理', 'C', '/system/menu',  'system/menu/index',   'system:menu:list',            'Menu',     3, 1, 1, 0, 0),
 (104, 100, '权限点',   'C', '/system/permission', 'system/permission/index', 'system:permission:list', 'Key', 4, 1, 1, 0, 0),
 (105, 100, '字典管理', 'C', '/system/dict',  'system/dict/index',   'system:dict:list',            'Book',     5, 1, 1, 0, 0),
 (106, 100, '参数配置', 'C', '/system/config','system/config/index', 'system:config:list',          'SetUp',    6, 1, 1, 0, 0),
 (107, 100, '公告管理', 'C', '/system/notice','system/notice/index', 'system:notice:list',          'Bell',     7, 1, 1, 0, 0),
 (108, 100, '文件管理', 'C', '/system/file',  'system/file/index',   'system:file:list',            'Folder',   8, 1, 1, 0, 0),
 (109, 100, '操作日志', 'C', '/system/oper-log', 'system/operlog/index', 'system:operlog:list',     'Document', 9, 1, 1, 0, 0),
 (110, 100, '登录日志', 'C', '/system/login-log','system/loginlog/index','system:loginlog:list',    'Clock',   10, 1, 1, 0, 0)
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
