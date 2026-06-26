-- M04 admin order list menu patch.
-- Execute this after the base gogorder admin menu script.
SET NAMES utf8mb4;

INSERT INTO sys_menu
SELECT 2050, '订单列表', 2000, 5, 'order', 'admin/order/index', '', '', 1, 0, 'C', '0', '0',
       'admin:order:list', 'list', 'admin', sysdate(), '', NULL, '管理后台订单查询与详情'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2050);

INSERT INTO sys_menu
SELECT 2051, '订单查询', 2050, 1, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:order:query', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2051);

UPDATE sys_menu
SET menu_name = '订单列表',
    parent_id = 2000,
    order_num = 5,
    path = 'order',
    component = 'admin/order/index',
    perms = 'admin:order:list',
    icon = 'list',
    remark = '管理后台订单查询与详情'
WHERE menu_id = 2050;

UPDATE sys_menu
SET menu_name = '订单查询',
    parent_id = 2050,
    order_num = 1,
    perms = 'admin:order:query'
WHERE menu_id = 2051;
