-- gogorder V1.0 product center schema patch
-- Safe to execute repeatedly on MySQL 8.0.

SET @stock_reason_exists = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'stock_ledger'
    AND column_name = 'reason'
);

SET @stock_reason_sql = IF(
  @stock_reason_exists = 0,
  'ALTER TABLE stock_ledger ADD COLUMN reason VARCHAR(200) DEFAULT '''' COMMENT ''后台库存调整原因'' AFTER idempotent_key',
  'SELECT 1'
);

PREPARE stock_reason_stmt FROM @stock_reason_sql;
EXECUTE stock_reason_stmt;
DEALLOCATE PREPARE stock_reason_stmt;
