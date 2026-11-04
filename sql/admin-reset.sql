-- 危险操作：清空菜单、权限点及其授权关系。
-- 保留 sys_user / sys_role / sys_user_role；执行后需要重新给角色勾选权限。
-- 使用顺序：
--   1) mysql -u root -p routego < sql/admin-reset.sql
--   2) mysql -u root -p routego < sql/admin-init.sql
-- 仅升版权限码时不需要执行本脚本，直接重跑 admin-init.sql 即可。

DELETE FROM sys_role_permission;
DELETE FROM sys_role_menu;
DELETE FROM sys_permission;
DELETE FROM sys_menu;

ALTER TABLE sys_permission AUTO_INCREMENT = 1;
ALTER TABLE sys_menu AUTO_INCREMENT = 1;
