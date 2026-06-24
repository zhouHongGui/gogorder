-- M15 门店员工账号管理菜单补丁。
-- 执行后在「运营管理」下新增「员工账号」页面入口。

SET NAMES utf8mb4;

INSERT INTO sys_menu
SELECT 2040, '员工账号', 2000, 4, 'staff', 'admin/staff/index', '', '', 1, 0, 'C', '0', '0',
       'admin:shop:staff', 'peoples', 'admin', sysdate(), '', NULL, '独立门店员工账号管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2040);

UPDATE sys_menu
SET menu_name = '员工账号',
    parent_id = 2000,
    order_num = 4,
    path = 'staff',
    component = 'admin/staff/index',
    perms = 'admin:shop:staff',
    icon = 'peoples',
    remark = '独立门店员工账号管理菜单'
WHERE menu_id = 2040;
