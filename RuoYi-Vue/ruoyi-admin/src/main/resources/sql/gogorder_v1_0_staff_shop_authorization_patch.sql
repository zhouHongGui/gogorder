-- M15 员工-门店授权模型调整：staff_shop 只保留授权门店关系，不再维护每店岗位。
SET NAMES utf8mb4;

SET @schema_name := DATABASE();
SET @drop_role_sql := (
  SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE `staff_shop` DROP COLUMN `role`',
    'SELECT ''legacy role column not exists'' AS message'
  )
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'staff_shop'
    AND column_name = 'role'
);

PREPARE stmt FROM @drop_role_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
