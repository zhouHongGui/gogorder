-- gogorder V1.0 business schema
-- Source of truth: docs/M01-数据库设计.md and docs/M13-全局枚举与接口契约.md
-- This script is intentionally non-destructive and can be executed repeatedly.
-- Existing databases created before M08 must execute gogorder_v1_0_order_schema_patch.sql
-- before deploying the order/payment code.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `c_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `phone` VARCHAR(11) NOT NULL COMMENT '手机号（唯一）',
  `nickname` VARCHAR(50) DEFAULT '',
  `avatar` VARCHAR(255) DEFAULT '',
  `status` TINYINT DEFAULT 1 COMMENT '0禁用 1正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C端用户';

CREATE TABLE IF NOT EXISTS `c_user_wechat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT 'c_user.id',
  `platform` VARCHAR(10) NOT NULL COMMENT 'MP=小程序 H5=公众号',
  `openid` VARCHAR(100) NOT NULL,
  `unionid` VARCHAR(100) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_openid` (`platform`, `openid`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信身份绑定';

CREATE TABLE IF NOT EXISTS `c_user_balance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `balance` INT NOT NULL DEFAULT 0 COMMENT '余额（分）',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户余额账户';

CREATE TABLE IF NOT EXISTS `balance_ledger` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(20) NOT NULL COMMENT 'RECHARGE/PAY/REFUND/ADJUST',
  `amount` INT NOT NULL COMMENT '金额（分），正=入账 负=出账',
  `before_balance` INT NOT NULL,
  `after_balance` INT NOT NULL,
  `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID',
  `operator_id` BIGINT DEFAULT NULL COMMENT '后台充值/调账操作人sys_user.id',
  `idempotent_key` VARCHAR(64) NOT NULL COMMENT '余额变更幂等键',
  `remark` VARCHAR(200) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotent` (`idempotent_key`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余额流水';

CREATE TABLE IF NOT EXISTS `payment_ledger` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `amount` INT NOT NULL COMMENT '支付金额（分）',
  `idempotent_key` VARCHAR(64) NOT NULL COMMENT '幂等键：{orderId}:pay',
  `status` TINYINT DEFAULT 1 COMMENT '1成功',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotent` (`idempotent_key`),
  UNIQUE KEY `uk_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水';

CREATE TABLE IF NOT EXISTS `refund_ledger` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `amount` INT NOT NULL COMMENT '退款金额（分）',
  `idempotent_key` VARCHAR(64) NOT NULL COMMENT '幂等键：{orderId}:refund（每单仅一次整单全额退款）',
  `status` TINYINT DEFAULT 1 COMMENT '1成功',
  `shop_id` BIGINT NOT NULL COMMENT '门店ID（审计：退款发生门店）',
  `operator_id` BIGINT NOT NULL COMMENT '操作人shop_staff.id（0=历史未知操作人）',
  `reason` VARCHAR(200) NOT NULL COMMENT '退款原因（必填，不可变审计）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotent` (`idempotent_key`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款流水';

CREATE TABLE IF NOT EXISTS `shop` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `shop_code` VARCHAR(20) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `image` VARCHAR(255) DEFAULT '',
  `phone` VARCHAR(20) NOT NULL,
  `province` VARCHAR(50) DEFAULT '',
  `city` VARCHAR(50) DEFAULT '',
  `district` VARCHAR(50) DEFAULT '',
  `address` VARCHAR(255) DEFAULT '',
  `longitude` DECIMAL(10,7) DEFAULT 0,
  `latitude` DECIMAL(10,7) DEFAULT 0,
  `open_time` TIME DEFAULT '10:00:00',
  `close_time` TIME DEFAULT '22:00:00' COMMENT '若<open_time表示跨午夜',
  `preorder_min_minutes` INT NOT NULL DEFAULT 30 COMMENT '最早可预约分钟数',
  `preorder_max_days` INT NOT NULL DEFAULT 7 COMMENT '最长可预约天数',
  `make_lead_minutes` INT NOT NULL DEFAULT 30 COMMENT '预订单提前进入制作窗口分钟数',
  `status` TINYINT DEFAULT 1 COMMENT '0休息 1营业 2暂停接单',
  `notice` VARCHAR(500) DEFAULT '',
  `pack_fee` INT DEFAULT 100 COMMENT '包装费（分/杯）',
  `sort_order` INT DEFAULT 0,
  `del_flag` TINYINT DEFAULT 0 COMMENT '0正常 1已删除（逻辑删除）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_code` (`shop_code`),
  KEY `idx_status_del` (`status`, `del_flag`),
  KEY `idx_location` (`longitude`, `latitude`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店（逻辑删除）';

CREATE TABLE IF NOT EXISTS `shop_staff` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account` VARCHAR(30) NOT NULL COMMENT '员工登录账号',
  `nickname` VARCHAR(50) NOT NULL COMMENT '员工姓名',
  `phone` VARCHAR(11) NOT NULL COMMENT '员工登录手机号',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt密码',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1停用',
  `token_version` INT NOT NULL DEFAULT 0 COMMENT '敏感信息变更时递增，旧JWT失效',
  `last_login_ip` VARCHAR(128) DEFAULT '',
  `last_login_time` DATETIME DEFAULT NULL,
  `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 2删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account` (`account`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_status_del` (`status`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='独立门店员工账号';

CREATE TABLE IF NOT EXISTS `staff_shop` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `staff_id` BIGINT NOT NULL COMMENT 'shop_staff.id',
  `shop_id` BIGINT NOT NULL,
  `is_default` TINYINT DEFAULT 0 COMMENT '1=默认门店',
  `default_staff_id` BIGINT GENERATED ALWAYS AS (
    CASE WHEN `is_default` = 1 THEN `staff_id` ELSE NULL END
  ) STORED COMMENT '用于保证每个员工仅一个默认门店',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_shop` (`staff_id`, `shop_id`),
  UNIQUE KEY `uk_staff_default` (`default_staff_id`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工-门店关联';

CREATE TABLE IF NOT EXISTS `shop_label_printer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `shop_id` BIGINT NOT NULL COMMENT '绑定门店ID',
  `sn` VARCHAR(64) NOT NULL COMMENT '飞鹅打印机编号',
  `printer_key` VARCHAR(128) NOT NULL COMMENT '飞鹅打印机密钥，仅后端保存',
  `printer_name` VARCHAR(100) DEFAULT '' COMMENT '设备备注名',
  `print_width` INT NOT NULL DEFAULT 40 COMMENT '标签宽度，毫米',
  `print_height` INT NOT NULL DEFAULT 50 COMMENT '标签高度，毫米',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  `feie_status` VARCHAR(100) DEFAULT '' COMMENT '飞鹅最后返回状态',
  `last_status_time` DATETIME DEFAULT NULL COMMENT '最后查询状态时间',
  `remark` VARCHAR(200) DEFAULT '',
  `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
  `active_sn` VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN `del_flag` = 0 THEN `sn` ELSE NULL END) STORED COMMENT '用于约束未删除设备SN唯一',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_active_sn` (`active_sn`),
  KEY `idx_shop_status` (`shop_id`, `status`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店标签打印机';

CREATE TABLE IF NOT EXISTS `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `sort_order` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

CREATE TABLE IF NOT EXISTS `product_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `category_id` BIGINT NOT NULL,
  `sort_order` INT DEFAULT 0 COMMENT '在该分类下的排序',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_category` (`product_id`, `category_id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品多分类关联';

CREATE TABLE IF NOT EXISTS `spec_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL COMMENT '规格名（杯型/温度/甜度/加料）',
  `type` TINYINT NOT NULL COMMENT '1单选 2多选',
  `is_required` TINYINT NOT NULL DEFAULT 1 COMMENT '0可选 1必选',
  `min_select` INT NOT NULL DEFAULT 1 COMMENT '最少选择数量',
  `max_select` INT NOT NULL DEFAULT 1 COMMENT '最多选择数量',
  `sort_order` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规格模板';

CREATE TABLE IF NOT EXISTS `spec_option` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `option_id` VARCHAR(32) NOT NULL COMMENT '全局唯一optionId（雪花ID/UUID）',
  `template_id` BIGINT NOT NULL COMMENT 'spec_template.id',
  `label` VARCHAR(50) NOT NULL COMMENT '选项名（中杯/大杯/…）',
  `price_add` INT DEFAULT 0 COMMENT '加价（分）',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认选中',
  `sort_order` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1 COMMENT '0禁用 1启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_option_id` (`option_id`),
  KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规格选项（optionId不可变）';

CREATE TABLE IF NOT EXISTS `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `image` VARCHAR(255) DEFAULT '',
  `images` JSON DEFAULT NULL COMMENT '详情图列表',
  `description` VARCHAR(500) DEFAULT '',
  `base_price` INT NOT NULL DEFAULT 0 COMMENT '基础价格（分）',
  `spec_template_ids` JSON DEFAULT NULL COMMENT '引用的规格模板ID列表',
  `tags` JSON DEFAULT NULL,
  `status` TINYINT DEFAULT 1 COMMENT '0下架 1上架',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品库';

CREATE TABLE IF NOT EXISTS `shop_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `shop_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `price` INT DEFAULT NULL COMMENT '门店售价（分），NULL=使用base_price',
  `status` TINYINT DEFAULT 1 COMMENT '0下架 1上架',
  `stock` INT DEFAULT -1 COMMENT '商品级库存，-1=无限',
  `sort_order` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_product` (`shop_id`, `product_id`),
  KEY `idx_shop_status` (`shop_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店商品';

CREATE TABLE IF NOT EXISTS `stock_ledger` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `shop_product_id` BIGINT NOT NULL,
  `change_type` VARCHAR(10) NOT NULL COMMENT 'DEDUCT/RESTORE/ADJUST',
  `change_amount` INT NOT NULL COMMENT '变更量（正=增 负=减）',
  `before_stock` INT NOT NULL,
  `after_stock` INT NOT NULL,
  `order_id` BIGINT DEFAULT NULL,
  `idempotent_key` VARCHAR(64) NOT NULL COMMENT '订单库存：{orderId}:{shopProductId}:{DEDUCT|RESTORE}；后台调整：adjust:{requestId}',
  `reason` VARCHAR(200) DEFAULT '' COMMENT '后台库存调整原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotent` (`idempotent_key`),
  KEY `idx_shop_product_id` (`shop_product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水';

CREATE TABLE IF NOT EXISTS `biz_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
  `submit_key` VARCHAR(64) NOT NULL COMMENT '客户端提交幂等键',
  `pickup_token` VARCHAR(12) DEFAULT NULL COMMENT '内部核销令牌（支付成功后生成）',
  `pickup_display` VARCHAR(5) DEFAULT NULL COMMENT '门店取餐日展示号（字母+3位数字，支付成功后生成）',
  `pickup_date` DATE DEFAULT NULL COMMENT '取餐日期，支付成功后生成',
  `user_id` BIGINT NOT NULL,
  `shop_id` BIGINT NOT NULL,
  `order_type` VARCHAR(10) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL即时单 PREORDER预订单',
  `scheduled_pickup_time` DATETIME DEFAULT NULL COMMENT '预订单预约取餐时间',
  `order_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1已接单待制作 2制作中 3待取餐 4已完成 5已取消',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1成功 2超时 3退款中 4已退款',
  `refund_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0无退款 1处理中 2成功 3失败',
  `product_amount` INT NOT NULL DEFAULT 0 COMMENT '商品总额（分）',
  `pack_fee` INT NOT NULL DEFAULT 0 COMMENT '包装费（分）',
  `total_amount` INT NOT NULL DEFAULT 0 COMMENT '实付金额（分）',
  `remark` VARCHAR(200) DEFAULT '',
  `pay_time` DATETIME DEFAULT NULL,
  `accept_time` DATETIME DEFAULT NULL,
  `make_start_time` DATETIME DEFAULT NULL,
  `complete_make_time` DATETIME DEFAULT NULL,
  `verify_time` DATETIME DEFAULT NULL,
  `cancel_time` DATETIME DEFAULT NULL,
  `cancel_reason` VARCHAR(200) DEFAULT '',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_user_submit_key` (`user_id`, `submit_key`),
  UNIQUE KEY `uk_pickup_token` (`pickup_token`),
  UNIQUE KEY `uk_pickup_display` (`shop_id`, `pickup_date`, `pickup_display`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_pending` (`user_id`, `order_status`, `pay_status`),
  KEY `idx_shop_order_status` (`shop_id`, `order_status`),
  KEY `idx_shop_pay_time` (`shop_id`, `pay_status`, `pay_time`, `id`),
  KEY `idx_shop_scheduled_pickup` (`shop_id`, `scheduled_pickup_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_timeout_sweep` (`order_status`, `pay_status`, `id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

CREATE TABLE IF NOT EXISTS `biz_order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `shop_product_id` BIGINT NOT NULL COMMENT '门店商品ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称（快照）',
  `product_image` VARCHAR(255) DEFAULT '',
  `specs` JSON NOT NULL COMMENT '规格快照数组：[{templateId, templateName, optionId, label, priceAdd}]',
  `unit_price` INT NOT NULL COMMENT '单价（分）',
  `quantity` INT NOT NULL DEFAULT 1,
  `subtotal` INT NOT NULL COMMENT '小计（分）',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_product` (`order_id`, `product_id`),
  KEY `idx_shop_product_id` (`shop_product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

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
