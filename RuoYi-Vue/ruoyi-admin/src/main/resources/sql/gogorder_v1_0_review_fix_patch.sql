-- Code review fixes for an existing V1.0 database. Execute once.

ALTER TABLE `biz_order`
  ADD KEY `idx_user_pending` (`user_id`, `order_status`, `pay_status`),
  ADD KEY `idx_shop_pay_time` (`shop_id`, `pay_status`, `pay_time`, `id`),
  ADD KEY `idx_timeout_sweep` (`order_status`, `pay_status`, `id`, `create_time`);

ALTER TABLE `biz_order_item`
  ADD KEY `idx_order_product` (`order_id`, `product_id`);
