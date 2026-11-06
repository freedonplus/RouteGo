-- ---------------------------------------------------------------------------
-- 工单「指派处理人」权限收紧
--
-- 变更点：指派处理人只保留给「超级管理员」「系统管理员」，
--         「客服」角色不再拥有 operations:CsTicket:assign。
--         客服仍可通过回复客户自动认领工单，以及走状态流转（开始处理/标记解决）。
--
-- 幂等：可重复执行。先删掉客服角色上的该权限，再确保超管/系统管理员有该权限。
-- 生效：权限按用户缓存并带全局版本号，改完需让缓存失效——
--       在「角色管理」里保存一次角色权限（会自动 +1 版本），
--       或执行 redis-cli DEL admin:config:SaToken:getPermVer 后让相关账号重新登录。
-- ---------------------------------------------------------------------------

-- 1. 客服角色：移除指派权限
DELETE rp
FROM sys_role_permission rp
         JOIN sys_role r ON r.id = rp.role_id
         JOIN sys_permission p ON p.id = rp.permission_id
WHERE r.role_code = 'support'
  AND p.permission_code = 'operations:CsTicket:assign';

-- 2. 超级管理员 / 系统管理员：补回指派权限（已存在则跳过）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code IN ('super_admin', 'admin')
  AND r.deleted = 0
  AND p.permission_code = 'operations:CsTicket:assign'
  AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission x
                  WHERE x.role_id = r.id AND x.permission_id = p.id);

-- 3. 自检：应只剩超管与系统管理员两条
-- SELECT r.role_name, p.permission_code
-- FROM sys_role_permission rp
--          JOIN sys_role r ON r.id = rp.role_id
--          JOIN sys_permission p ON p.id = rp.permission_id
-- WHERE p.permission_code = 'operations:CsTicket:assign';
