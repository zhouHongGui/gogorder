-- M08 order and payment schema patch. Execute once for an existing V1.0 database.

-- 先允许 NULL，给已有订单明细回填门店商品ID后再收紧约束。
ALTER TABLE `biz_order_item`
  ADD COLUMN `shop_product_id` BIGINT NULL COMMENT '门店商品ID' AFTER `product_id`;

UPDATE `biz_order_item` bi
INNER JOIN `biz_order` bo ON bo.id = bi.order_id
INNER JOIN `shop_product` sp ON sp.shop_id = bo.shop_id AND sp.product_id = bi.product_id
SET bi.shop_product_id = sp.id
WHERE bi.shop_product_id IS NULL;

-- 执行前应确认不存在无法匹配门店商品的历史明细：
-- SELECT bi.id FROM biz_order_item bi WHERE bi.shop_product_id IS NULL;
ALTER TABLE `biz_order_item`
  MODIFY COLUMN `shop_product_id` BIGINT NOT NULL COMMENT '门店商品ID',
  ADD KEY `idx_shop_product_id` (`shop_product_id`);

CREATE TABLE IF NOT EXISTS `pickup_sequence` (
  `shop_id` BIGINT NOT NULL,
  `pickup_date` DATE NOT NULL,
  `current_seq` INT NOT NULL DEFAULT 0 COMMENT '当前最大展示号',
  PRIMARY KEY (`shop_id`, `pickup_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='取餐展示号序列';

INSERT INTO `sys_job` (
  `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`,
  `concurrent`, `status`, `create_by`, `create_time`, `remark`
)
SELECT '待支付订单超时取消', 'SYSTEM', 'paymentTimeoutTask.cancelTimeoutOrders', '0 * * * * ?',
       '3', '1', '0', 'admin', SYSDATE(), '每分钟取消超过15分钟仍未支付的订单'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_job` WHERE `invoke_target` = 'paymentTimeoutTask.cancelTimeoutOrders'
);
