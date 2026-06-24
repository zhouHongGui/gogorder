-- refund_ledger 审计列既有库迁移（shop_id / operator_id / reason + 索引）
--
-- 背景：RefundLedgerMapper.xml 的 INSERT 已写入 shop_id/operator_id/reason 三列，
--   fresh schema（gogorder_v1_0_business_schema.sql 的 CREATE TABLE）也已含这三列；
--   但既有库由旧版 business_schema 建表，缺少这三列，走员工端退款会 SQL 报错。
--   本 patch 给既有库补列，幂等（已迁移则全部跳过，可重复执行）。
--
-- 语义：operator_id = shop_staff.id（门店退款操作人，0=历史未知）；与 balance_ledger.operator_id
--   （后台充值/调账，sys_user.id）是不同 ID 空间，勿混用。
SET NAMES utf8mb4;

-- 1. 若 shop_id 列不存在，则以可空形式一次性添加三列（兼容历史数据，避免 NOT NULL 直接加失败）
SET @has_shop_id := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'refund_ledger' AND column_name = 'shop_id'
);
SET @ddl_add_cols := IF(@has_shop_id = 0,
  'ALTER TABLE `refund_ledger` '
    'ADD COLUMN `shop_id` BIGINT NULL COMMENT ''门店ID（审计：退款发生门店）'' AFTER `status`, '
    'ADD COLUMN `operator_id` BIGINT NULL COMMENT ''操作人 shop_staff.id；0=历史未知操作人'' AFTER `shop_id`, '
    'ADD COLUMN `reason` VARCHAR(200) NULL COMMENT ''退款原因（必填，不可变审计）'' AFTER `operator_id`',
  'SELECT ''refund_ledger: audit columns already exist'' AS msg'
);
PREPARE stmt_add_cols FROM @ddl_add_cols;
EXECUTE stmt_add_cols;
DEALLOCATE PREPARE stmt_add_cols;

-- 2. 回填历史退款审计数据
--    shop_id     来自 biz_order.shop_id（退款必有订单，可回填）
--    operator_id 历史退款操作人无法追溯，统一填 0（与注释一致）
--    reason      优先用现有值，其次订单取消原因，最终占位“历史退款”
UPDATE `refund_ledger` r
INNER JOIN `biz_order` o ON o.id = r.order_id
SET r.shop_id     = COALESCE(r.shop_id, o.shop_id),
    r.operator_id = COALESCE(r.operator_id, 0),
    r.reason      = COALESCE(NULLIF(TRIM(r.reason), ''), NULLIF(TRIM(o.cancel_reason), ''), '历史退款')
WHERE r.shop_id IS NULL
   OR r.operator_id IS NULL
   OR r.reason IS NULL
   OR TRIM(r.reason) = '';

-- 3. 收紧为 NOT NULL（幂等：已是 NOT NULL 再次 MODIFY 无副作用）
ALTER TABLE `refund_ledger`
  MODIFY COLUMN `shop_id` BIGINT NOT NULL COMMENT '门店ID（审计：退款发生门店）',
  MODIFY COLUMN `operator_id` BIGINT NOT NULL COMMENT '操作人 shop_staff.id（0=历史未知操作人）',
  MODIFY COLUMN `reason` VARCHAR(200) NOT NULL COMMENT '退款原因（必填，不可变审计）';

-- 4. 补索引（幂等：不存在才加；索引名与 business_schema 保持一致）
SET @has_idx_shop := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'refund_ledger' AND index_name = 'idx_shop_id'
);
SET @ddl_add_idx := IF(@has_idx_shop = 0,
  'ALTER TABLE `refund_ledger` ADD INDEX `idx_shop_id` (`shop_id`), ADD INDEX `idx_operator_id` (`operator_id`)',
  'SELECT ''refund_ledger: indexes already exist'' AS msg'
);
PREPARE stmt_add_idx FROM @ddl_add_idx;
EXECUTE stmt_add_idx;
DEALLOCATE PREPARE stmt_add_idx;
