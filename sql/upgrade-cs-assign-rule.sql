-- =============================================================================
-- 客服工单自动分配（可重复执行）
--
-- 用途：已有库升级。新库直接执行 routego.sql + admin-init.sql 即可，无需本脚本。
-- 执行：mysql -u root -p routego < sql/upgrade-cs-assign-rule.sql
--
-- 内容：
--   1. cs_ticket 增加 assign_rule_id，记录命中的规则，便于回溯「这单是谁分的」
--   2. 新建 cs_assign_rule 规则表
--   3. 补齐 5 个权限点，并给「客服」角色开只读（配置规则仍由管理员操作）
-- =============================================================================

DROP PROCEDURE IF EXISTS rg_add_column;

DELIMITER $$
CREATE PROCEDURE rg_add_column(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(600))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN ', ddl);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL rg_add_column('cs_ticket', 'assign_rule_id',
  '`assign_rule_id` bigint DEFAULT NULL COMMENT ''自动分配命中的规则 ID，人工指派时为空''');

DROP PROCEDURE IF EXISTS rg_add_column;

CREATE TABLE IF NOT EXISTS cs_assign_rule (
  id             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  rule_name      varchar(50)  NOT NULL COMMENT '规则名称',
  match_type     tinyint      DEFAULT NULL COMMENT '适用工单类型：1 售前，2 订单，3 退款，4 投诉，5 其他；NULL 不限',
  match_priority tinyint      DEFAULT NULL COMMENT '适用优先级：1 低，2 中，3 高；NULL 不限',
  strategy       tinyint      NOT NULL DEFAULT 2 COMMENT '分配策略：1 轮询，2 当前在处理最少',
  user_ids       varchar(500) NOT NULL COMMENT '候选客服 ID，逗号分隔',
  enabled        tinyint      NOT NULL DEFAULT 1 COMMENT '是否启用：0 停用，1 启用',
  sort           int          NOT NULL DEFAULT 0 COMMENT '匹配顺序，数字小的先命中',
  remark         varchar(255) DEFAULT NULL COMMENT '备注',
  version        int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  create_by      bigint       DEFAULT NULL COMMENT '创建人 ID',
  create_time    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by      bigint       DEFAULT NULL COMMENT '更新人 ID',
  update_time    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted        tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  KEY idx_enabled_sort (enabled, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客服自动分配规则表';

-- 权限点：permission_code 有唯一索引，用 NOT EXISTS 保证可重复执行
INSERT INTO sys_permission (permission_name, permission_code, module, description, status, deleted)
SELECT * FROM (
  SELECT '规则分页' AS a, 'operations:CsAssignRule:page' AS b, '自动分配规则' AS c,
         '查看分配规则、启停状态与启用顺序，并用试算接口预演某类工单会分给谁（只读）' AS d, 1 AS e, 0 AS f
  UNION ALL SELECT '新建规则', 'operations:CsAssignRule:create', '自动分配规则',
         '按工单类型/优先级圈定一组客服，选择轮询或「在处理最少」策略；至少一个可用客服才允许保存', 1, 0
  UNION ALL SELECT '编辑规则', 'operations:CsAssignRule:update', '自动分配规则',
         '修改匹配条件、候选客服、策略与排序；带乐观锁，并发修改会提示刷新后重试', 1, 0
  UNION ALL SELECT '删除规则', 'operations:CsAssignRule:delete', '自动分配规则',
         '逻辑删除规则，之后的新工单不再命中；已分配工单的 assign_rule_id 仍可追溯', 1, 0
  UNION ALL SELECT '启停规则', 'operations:CsAssignRule:updateStatus', '自动分配规则',
         '临时停用某条规则而不删除，常用于客服请假期间把该组规则摘下', 1, 0
) t
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.permission_code = t.b
);

-- 客服角色（role_id = 3）只读，配置规则交给管理员
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, p.id
FROM sys_permission p
WHERE p.permission_code = 'operations:CsAssignRule:page'
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 3 AND rp.permission_id = p.id
  );
