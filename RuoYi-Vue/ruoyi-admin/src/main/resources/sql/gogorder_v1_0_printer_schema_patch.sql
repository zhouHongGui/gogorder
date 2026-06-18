-- gogorder V1.0 printer schema patch
-- 本脚本用于在已有数据库上新增"门店标签打印机"表，启用飞鹅标签打印菜单前执行。

-- 统一使用 utf8mb4 字符集，保证中文和 emoji 正常存储
SET NAMES utf8mb4;

-- 门店标签打印机表：每行记录一台绑定到飞鹅云打印的标签打印机
CREATE TABLE IF NOT EXISTS `shop_label_printer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, -- 主键自增
  `shop_id` BIGINT NOT NULL COMMENT '绑定门店ID', -- 所属门店
  `sn` VARCHAR(64) NOT NULL COMMENT '飞鹅打印机编号', -- 设备 SN，6-32 位数字
  `printer_key` VARCHAR(128) NOT NULL COMMENT '飞鹅打印机密钥，仅后端保存', -- 设备密钥，不回显到前端列表
  `printer_name` VARCHAR(100) DEFAULT '' COMMENT '设备备注名', -- 可空，用于区分多台设备
  `print_width` INT NOT NULL DEFAULT 40 COMMENT '标签宽度，毫米', -- 标签纸宽度
  `print_height` INT NOT NULL DEFAULT 50 COMMENT '标签高度，毫米', -- 标签纸高度
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用', -- 启用状态
  `feie_status` VARCHAR(100) DEFAULT '' COMMENT '飞鹅最后返回状态', -- 最近一次飞鹅状态描述
  `last_status_time` DATETIME DEFAULT NULL COMMENT '最后查询状态时间', -- 最近查询状态时间
  `remark` VARCHAR(200) DEFAULT '', -- 备注
  `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除', -- 逻辑删除标志
  -- 生成列：未删除时取 SN，已删除时为 NULL，从而配合唯一索引实现"仅未删除记录 SN 唯一"
  `active_sn` VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN `del_flag` = 0 THEN `sn` ELSE NULL END) STORED COMMENT '用于约束未删除设备SN唯一',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, -- 创建时间默认当前时间
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 更新时间自动维护
  PRIMARY KEY (`id`), -- 主键
  UNIQUE KEY `uk_active_sn` (`active_sn`), -- 未删除设备 SN 唯一（借助生成列 active_sn）
  KEY `idx_shop_status` (`shop_id`, `status`, `del_flag`) -- 门店+状态+删除标志联合索引，加速订单打印查询
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店标签打印机'; -- 表注释
