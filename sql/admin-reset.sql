-- =============================================================================
-- RouteGo 后台权限数据 · 全量重置
-- 用途：把「用户 / 角色 / 菜单 / 权限点 / 字典 / 参数 / 公告 / 日志」恢复到空状态，
--       执行完必须紧接着执行 sql/admin-init.sql 写入标准初始化数据。
--
-- 使用顺序：
--   1) mysql -uroot -p routego < sql/admin-reset.sql
--   2) mysql -uroot -p routego < sql/admin-init.sql
--   3) 清 Redis 缓存（权限缓存带版本号，不清会读到旧权限）：
--      redis-cli -a <密码> --scan --pattern 'admin:*' | xargs -r redis-cli -a <密码> del
--
-- 影响范围：
--   * 会删除所有后台账号、角色、菜单、权限点及其关联关系，不可恢复。
--   * 会清空登录日志、操作日志、文件元数据（磁盘上的 uploads 目录不会删除）。
--   * 不会动业务表（route / orders / customer 等），业务数据请另行处理。
--
-- 仅升级权限码（保留账号与角色）时不要执行本脚本，直接重跑 admin-init.sql。
-- =============================================================================

SET NAMES utf8mb4;

-- 关闭外键检查：本库按约定不建物理外键，这里只是兜底，避免历史环境残留约束报错
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 1. 关系表（先删关系，再删主体）
-- ---------------------------------------------------------------------------
DELETE FROM sys_role_permission;   -- 角色 ↔ 权限点
DELETE FROM sys_role_menu;         -- 角色 ↔ 菜单
DELETE FROM sys_user_role;         -- 用户 ↔ 角色
DELETE FROM sys_user_oauth;        -- 用户 ↔ 第三方账号

-- ---------------------------------------------------------------------------
-- 2. 主体表
-- ---------------------------------------------------------------------------
DELETE FROM sys_permission;        -- 权限点
DELETE FROM sys_menu;              -- 菜单
DELETE FROM sys_role;              -- 角色
DELETE FROM sys_user;              -- 后台用户

-- ---------------------------------------------------------------------------
-- 3. 配置与内容
-- ---------------------------------------------------------------------------
DELETE FROM sys_dict_data;         -- 字典项
DELETE FROM sys_dict_type;         -- 字典类型
DELETE FROM sys_config;            -- 系统参数
DELETE FROM sys_notice;            -- 公告

-- ---------------------------------------------------------------------------
-- 4. 日志与文件（可选：想保留审计记录时注释掉本节）
-- ---------------------------------------------------------------------------
DELETE FROM sys_login_log;         -- 登录日志
DELETE FROM sys_oper_log;          -- 操作日志
DELETE FROM sys_file;              -- 文件元数据

-- ---------------------------------------------------------------------------
-- 5. 重置自增，保证初始化脚本分配的 ID 可预期
-- ---------------------------------------------------------------------------
ALTER TABLE sys_role            AUTO_INCREMENT = 1;
ALTER TABLE sys_user            AUTO_INCREMENT = 1;
ALTER TABLE sys_permission      AUTO_INCREMENT = 1;
ALTER TABLE sys_menu            AUTO_INCREMENT = 1;
ALTER TABLE sys_dict_type       AUTO_INCREMENT = 1;
ALTER TABLE sys_dict_data       AUTO_INCREMENT = 1;
ALTER TABLE sys_config          AUTO_INCREMENT = 1;
ALTER TABLE sys_notice          AUTO_INCREMENT = 1;
ALTER TABLE sys_role_permission AUTO_INCREMENT = 1;
ALTER TABLE sys_role_menu       AUTO_INCREMENT = 1;
ALTER TABLE sys_user_role       AUTO_INCREMENT = 1;
ALTER TABLE sys_user_oauth      AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;
