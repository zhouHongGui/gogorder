-- 制作队列与自动完成 schema patch（2026-06-24，见 M15 §6.4 / M13 §5.2）
-- 1. biz_order 加 estimated_ready_time（支付时按串行队列估算的预计取餐时间）
-- 2. shop 加 minutes_per_cup（单杯制作时间，默认 3 分钟）
-- 3. 补齐制作调度 / 待取餐超时 / 每日兜底归档 Quartz 任务
-- 既有库迁移；fresh 库由 business_schema.sql 直接建全。
-- 若历史库已经存在 concurrent_capacity，本版本保留该列但代码不再读取。

SET NAMES utf8mb4;

-- 1. biz_order 加预计取餐时间
SET @has_est := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_order' AND column_name = 'estimated_ready_time');
SET @ddl_est := IF(@has_est = 0,
  'ALTER TABLE `biz_order` ADD COLUMN `estimated_ready_time` DATETIME DEFAULT NULL COMMENT ''预计取餐时间（支付时按串行队列估算）'' AFTER `cancel_reason`',
  'SELECT ''biz_order: estimated_ready_time already exists'' AS msg');
PREPARE stmt_est FROM @ddl_est; EXECUTE stmt_est; DEALLOCATE PREPARE stmt_est;

-- 2. shop 加单杯制作时间（不再新增 concurrent_capacity）
SET @has_minutes := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'shop' AND column_name = 'minutes_per_cup');
SET @ddl_minutes := IF(@has_minutes = 0,
  'ALTER TABLE `shop` ADD COLUMN `minutes_per_cup` INT NOT NULL DEFAULT 3 COMMENT ''单杯制作时间（分钟）'' AFTER `make_lead_minutes`',
  'ALTER TABLE `shop` MODIFY COLUMN `minutes_per_cup` INT NOT NULL DEFAULT 3 COMMENT ''单杯制作时间（分钟）''');
PREPARE stmt_minutes FROM @ddl_minutes; EXECUTE stmt_minutes; DEALLOCATE PREPARE stmt_minutes;

-- 3. 补索引：待取餐 4 小时自动完成、凌晨兜底归档
SET @has_ready_idx := (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_order' AND index_name = 'idx_ready_timeout');
SET @ddl_ready_idx := IF(@has_ready_idx = 0,
  'ALTER TABLE `biz_order` ADD INDEX `idx_ready_timeout` (`order_status`, `pay_status`, `complete_make_time`, `id`)',
  'SELECT ''biz_order: idx_ready_timeout already exists'' AS msg');
PREPARE stmt_ready_idx FROM @ddl_ready_idx; EXECUTE stmt_ready_idx; DEALLOCATE PREPARE stmt_ready_idx;

SET @has_finalize_idx := (SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_order' AND index_name = 'idx_daily_finalize');
SET @ddl_finalize_idx := IF(@has_finalize_idx = 0,
  'ALTER TABLE `biz_order` ADD INDEX `idx_daily_finalize` (`order_status`, `pay_status`, `pickup_date`, `id`)',
  'SELECT ''biz_order: idx_daily_finalize already exists'' AS msg');
PREPARE stmt_finalize_idx FROM @ddl_finalize_idx; EXECUTE stmt_finalize_idx; DEALLOCATE PREPARE stmt_finalize_idx;

-- 4. 制作调度兜底任务：每分钟串行推进一个可制作订单
INSERT INTO `sys_job` (
  `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`,
  `concurrent`, `status`, `create_by`, `create_time`, `remark`
)
SELECT '制作调度兜底扫描', 'SYSTEM', 'productionSweepTask.sweep', '0 * * * * ?',
       '3', '1', '0', 'admin', SYSDATE(), '每分钟串行推进队列订单进入制作（M15 §6.4）'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_job` WHERE `invoke_target` = 'productionSweepTask.sweep'
);

-- 5. 待取餐超时自动完成：每分钟处理 READY(3) 且出餐超过 4 小时的订单
INSERT INTO `sys_job` (
  `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`,
  `concurrent`, `status`, `create_by`, `create_time`, `remark`
)
SELECT '待取餐超时自动完成', 'SYSTEM', 'pickupTimeoutTask.completeTimeoutReadyOrders', '0 * * * * ?',
       '3', '1', '0', 'admin', SYSDATE(), '每分钟将待取餐超过4小时订单自动归档完成（M15 §6.4）'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_job` WHERE `invoke_target` = 'pickupTimeoutTask.completeTimeoutReadyOrders'
);

-- 6. 每日兜底归档：凌晨 4 点将昨日及更早仍未完成的 1/2/3 订单归档为完成
INSERT INTO `sys_job` (
  `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`,
  `concurrent`, `status`, `create_by`, `create_time`, `remark`
)
SELECT '每日订单兜底归档', 'SYSTEM', 'dailyFinalizeTask.finalizePreviousDayOrders', '0 0 4 * * ?',
       '3', '1', '0', 'admin', SYSDATE(), '每天凌晨4点将昨日及更早未完成订单归档完成（M15 §6.4）'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_job` WHERE `invoke_target` = 'dailyFinalizeTask.finalizePreviousDayOrders'
);
