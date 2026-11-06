-- =============================================================================
-- 客服会话增强：未读数、最后一条消息（可重复执行）
--
-- 用途：已有库升级。新库直接执行 routego.sql 即可，无需本脚本。
-- 执行：mysql -u root -p routego < sql/upgrade-cs-chat.sql
--
-- 说明：MySQL 8 的 ADD COLUMN 不支持 IF NOT EXISTS，这里用存储过程查
-- information_schema 判断列是否存在，重复执行不会报错。
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

CALL rg_add_column('cs_ticket', 'last_message_time',
  '`last_message_time` datetime DEFAULT NULL COMMENT ''最后一条消息时间，会话列表按它排序''');
CALL rg_add_column('cs_ticket', 'last_message',
  '`last_message` varchar(200) DEFAULT NULL COMMENT ''最后一条消息摘要，会话列表直接展示''');
CALL rg_add_column('cs_ticket', 'customer_unread',
  '`customer_unread` int NOT NULL DEFAULT 0 COMMENT ''客户未读数：客服回复后 +1，客户打开会话清零''');
CALL rg_add_column('cs_ticket', 'staff_unread',
  '`staff_unread` int NOT NULL DEFAULT 0 COMMENT ''客服未读数：客户发消息后 +1，客服打开会话清零''');

DROP PROCEDURE IF EXISTS rg_add_column;

-- 会话列表排序索引，同样只在缺失时创建
SET @has_index = (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cs_ticket' AND INDEX_NAME = 'idx_last_message_time'
);
SET @ddl = IF(@has_index = 0,
  'ALTER TABLE `cs_ticket` ADD KEY `idx_last_message_time` (`last_message_time`)',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 回填历史会话的最后消息：老数据只有消息表，没有摘要字段
UPDATE cs_ticket t
JOIN (
  SELECT m.ticket_id, m.content, m.create_time
  FROM cs_message m
  JOIN (
    SELECT ticket_id, MAX(id) AS max_id FROM cs_message GROUP BY ticket_id
  ) latest ON latest.max_id = m.id
) last ON last.ticket_id = t.id
SET t.last_message_time = last.create_time,
    t.last_message = LEFT(REPLACE(REPLACE(last.content, '\r', ' '), '\n', ' '), 100)
WHERE t.last_message_time IS NULL;
