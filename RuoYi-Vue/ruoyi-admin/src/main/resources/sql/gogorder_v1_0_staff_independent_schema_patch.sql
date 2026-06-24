-- M15 门店员工独立账号迁移（从 sys_user + staff_login + staff_shop.user_id 迁移）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `shop_staff` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account` VARCHAR(30) NOT NULL COMMENT '员工登录账号',
  `nickname` VARCHAR(50) NOT NULL COMMENT '员工姓名',
  `phone` VARCHAR(11) NOT NULL COMMENT '员工登录手机号',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt密码',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1停用',
  `token_version` INT NOT NULL DEFAULT 0,
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

-- 已配置 staff_login 的若依用户迁移为独立员工；密码沿用原 BCrypt 值。
INSERT IGNORE INTO `shop_staff` (`account`, `nickname`, `phone`, `password`, `status`, `token_version`, `del_flag`)
SELECT u.user_name, u.nick_name, sl.phone, u.password,
       CASE WHEN u.status = '0' THEN 0 ELSE 1 END, 0, 0
FROM `staff_login` sl
INNER JOIN `sys_user` u ON u.user_id = sl.user_id AND u.del_flag = '0';

-- 旧 staff_shop 中可能存在未配置 staff_login 的员工，使用其若依手机号补迁，避免门店关系丢失。
INSERT IGNORE INTO `shop_staff` (`account`, `nickname`, `phone`, `password`, `status`, `token_version`, `del_flag`)
SELECT DISTINCT u.user_name, u.nick_name, u.phonenumber, u.password,
       CASE WHEN u.status = '0' THEN 0 ELSE 1 END, 0, 0
FROM `staff_shop` old
INNER JOIN `sys_user` u ON u.user_id = old.user_id AND u.del_flag = '0'
LEFT JOIN `staff_login` sl ON sl.user_id = u.user_id
WHERE sl.user_id IS NULL AND u.phonenumber REGEXP '^1[3-9][0-9]{9}$';

CREATE TABLE `staff_shop_new` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `staff_id` BIGINT NOT NULL COMMENT 'shop_staff.id',
  `shop_id` BIGINT NOT NULL,
  `is_default` TINYINT DEFAULT 0,
  `default_staff_id` BIGINT GENERATED ALWAYS AS (
    CASE WHEN `is_default` = 1 THEN `staff_id` ELSE NULL END
  ) STORED,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_shop` (`staff_id`, `shop_id`),
  UNIQUE KEY `uk_staff_default` (`default_staff_id`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工-门店关联';

-- 仅迁移已能映射到独立员工账号的旧绑定；执行前应检查两边行数。
INSERT INTO `staff_shop_new` (`staff_id`, `shop_id`, `is_default`, `create_time`)
SELECT ns.id, old.shop_id, old.is_default, old.create_time
FROM `staff_shop` old
INNER JOIN `sys_user` u ON u.user_id = old.user_id
INNER JOIN `shop_staff` ns ON ns.account = u.user_name;

DROP TABLE `staff_shop`;
RENAME TABLE `staff_shop_new` TO `staff_shop`;
DROP TABLE IF EXISTS `staff_login`;
