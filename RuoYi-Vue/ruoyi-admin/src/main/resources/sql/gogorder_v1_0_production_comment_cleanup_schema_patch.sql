-- 制作流转注释清理 patch（2026-06-25，见 M15 §6.4 / M13 §5.2）
-- 默认只清理字段 COMMENT 与 sys_job.remark，不删除历史字段。
-- 如确认旧库和报表均不再依赖 shop.concurrent_capacity，可将下方开关改为 1 后执行。

SET NAMES utf8mb4;

SET @drop_concurrent_capacity := 0;

-- 1. biz_order 字段语义更新：pickup_token 仅作为出餐兜底扫码码；verify_time 复用为完成/归档时间。
ALTER TABLE `biz_order`
  MODIFY COLUMN `pickup_token` VARCHAR(12) DEFAULT NULL COMMENT '出餐兜底扫码码（支付成功后生成）',
  MODIFY COLUMN `complete_make_time` DATETIME DEFAULT NULL COMMENT '通知取餐时间（2→3，制作完成）',
  MODIFY COLUMN `verify_time` DATETIME DEFAULT NULL COMMENT '完成/归档时间（3→4，历史字段名）',
  MODIFY COLUMN `estimated_ready_time` DATETIME DEFAULT NULL COMMENT '预计取餐时间（支付时按串行队列估算）';

-- 2. shop 制作配置语义更新：串行模式只保留 minutes_per_cup；concurrent_capacity 如存在默认保留。
ALTER TABLE `shop`
  MODIFY COLUMN `minutes_per_cup` INT NOT NULL DEFAULT 3 COMMENT '单杯制作时间（分钟）';

-- 3. sys_job 备注更新，清理旧“令牌桶/产能”表述。
UPDATE `sys_job`
SET `remark` = '每分钟串行推进队列订单进入制作（M15 §6.4）'
WHERE `invoke_target` = 'productionSweepTask.sweep';

UPDATE `sys_job`
SET `remark` = '每分钟将待取餐超过4小时订单自动归档完成（M15 §6.4）'
WHERE `invoke_target` = 'pickupTimeoutTask.completeTimeoutReadyOrders';

UPDATE `sys_job`
SET `remark` = '每天凌晨4点将昨日及更早未完成订单归档完成（M15 §6.4）'
WHERE `invoke_target` = 'dailyFinalizeTask.finalizePreviousDayOrders';

-- 4. 可选删除历史并发产能字段。默认不删；将 @drop_concurrent_capacity 改为 1 后才会 DROP。
SET @has_concurrent_capacity := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'shop'
    AND column_name = 'concurrent_capacity'
);
SET @ddl_drop_concurrent_capacity := IF(
  @drop_concurrent_capacity = 1 AND @has_concurrent_capacity > 0,
  'ALTER TABLE `shop` DROP COLUMN `concurrent_capacity`',
  'SELECT ''shop.concurrent_capacity kept (set @drop_concurrent_capacity := 1 to drop)'' AS msg'
);
PREPARE stmt_drop_concurrent_capacity FROM @ddl_drop_concurrent_capacity;
EXECUTE stmt_drop_concurrent_capacity;
DEALLOCATE PREPARE stmt_drop_concurrent_capacity;
