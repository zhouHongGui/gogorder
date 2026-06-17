-- gogorder V1.0 management menu
-- Idempotent inserts for the shop management module.

SET NAMES utf8mb4;

INSERT INTO sys_menu
SELECT 2000, '运营管理', 0, 1, 'operations', NULL, '', '', 1, 0, 'M', '0', '0', '', 'shopping',
       'admin', sysdate(), '', NULL, 'gogorder运营管理目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2000);

INSERT INTO sys_menu
SELECT 2001, '门店管理', 2000, 1, 'shop', 'admin/shop/index', '', '', 1, 0, 'C', '0', '0',
       'admin:shop:list', 'shop', 'admin', sysdate(), '', NULL, '门店管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2001);

INSERT INTO sys_menu
SELECT 2002, '门店查询', 2001, 1, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:shop:query', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2002);

INSERT INTO sys_menu
SELECT 2003, '门店新增', 2001, 2, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:shop:add', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2003);

INSERT INTO sys_menu
SELECT 2004, '门店修改', 2001, 3, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:shop:edit', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2004);

INSERT INTO sys_menu
SELECT 2005, '门店删除', 2001, 4, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:shop:remove', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2005);

INSERT INTO sys_menu
SELECT 2006, '门店状态', 2001, 5, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:shop:status', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2006);

INSERT INTO sys_menu
SELECT 2007, '门店员工', 2001, 6, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:shop:staff', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2007);

INSERT INTO sys_menu
SELECT 2010, '商品中心', 2000, 2, 'product', 'admin/product/index', '', '', 1, 0, 'C', '0', '0',
       'admin:product:list', 'product', 'admin', sysdate(), '', NULL, '商品中心管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2010);

INSERT INTO sys_menu
SELECT 2011, '分类查询', 2010, 1, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:category:list', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2011);

INSERT INTO sys_menu
SELECT 2012, '分类维护', 2010, 2, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:category:add', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2012);

INSERT INTO sys_menu
SELECT 2013, '分类修改', 2010, 3, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:category:edit', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2013);

INSERT INTO sys_menu
SELECT 2014, '分类删除', 2010, 4, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:category:remove', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2014);

INSERT INTO sys_menu
SELECT 2015, '规格查询', 2010, 5, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:spec:list', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2015);

INSERT INTO sys_menu
SELECT 2016, '规格新增', 2010, 6, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:spec:add', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2016);

INSERT INTO sys_menu
SELECT 2017, '规格修改', 2010, 7, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:spec:edit', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2017);

INSERT INTO sys_menu
SELECT 2018, '规格删除', 2010, 8, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:spec:remove', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2018);

INSERT INTO sys_menu
SELECT 2019, '商品查询', 2010, 9, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:product:query', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2019);

INSERT INTO sys_menu
SELECT 2020, '商品新增', 2010, 10, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:product:add', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2020);

INSERT INTO sys_menu
SELECT 2021, '商品修改', 2010, 11, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:product:edit', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2021);

INSERT INTO sys_menu
SELECT 2022, '商品删除', 2010, 12, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:product:remove', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2022);

INSERT INTO sys_menu
SELECT 2023, '门店商品查询', 2010, 13, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:shop-product:list', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2023);

INSERT INTO sys_menu
SELECT 2024, '门店商品分配', 2010, 14, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:shop-product:assign', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2024);

INSERT INTO sys_menu
SELECT 2025, '门店商品修改', 2010, 15, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:shop-product:edit', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2025);

INSERT INTO sys_menu
SELECT 2026, '库存调整', 2010, 16, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:shop-product:stock', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2026);

INSERT INTO sys_menu
SELECT 2027, '门店商品移除', 2010, 17, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:shop-product:remove', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2027);

-- Keep existing menu names synchronized and repair data inserted with a non-UTF-8 client.
UPDATE sys_menu SET icon = 'shop' WHERE menu_id = 2001;
UPDATE sys_menu SET menu_name = '商品中心', icon = 'product', remark = '商品中心管理菜单' WHERE menu_id = 2010;
UPDATE sys_menu SET menu_name = '分类查询' WHERE menu_id = 2011;
UPDATE sys_menu SET menu_name = '分类维护' WHERE menu_id = 2012;
UPDATE sys_menu SET menu_name = '分类修改' WHERE menu_id = 2013;
UPDATE sys_menu SET menu_name = '分类删除' WHERE menu_id = 2014;
UPDATE sys_menu SET menu_name = '规格查询' WHERE menu_id = 2015;
UPDATE sys_menu SET menu_name = '规格新增' WHERE menu_id = 2016;
UPDATE sys_menu SET menu_name = '规格修改' WHERE menu_id = 2017;
UPDATE sys_menu SET menu_name = '规格删除' WHERE menu_id = 2018;
UPDATE sys_menu SET menu_name = '商品查询' WHERE menu_id = 2019;
UPDATE sys_menu SET menu_name = '商品新增' WHERE menu_id = 2020;
UPDATE sys_menu SET menu_name = '商品修改' WHERE menu_id = 2021;
UPDATE sys_menu SET menu_name = '商品删除' WHERE menu_id = 2022;
UPDATE sys_menu SET menu_name = '门店商品查询' WHERE menu_id = 2023;
UPDATE sys_menu SET menu_name = '门店商品分配' WHERE menu_id = 2024;
UPDATE sys_menu SET menu_name = '门店商品修改' WHERE menu_id = 2025;
UPDATE sys_menu SET menu_name = '库存调整' WHERE menu_id = 2026;
UPDATE sys_menu SET menu_name = '门店商品移除' WHERE menu_id = 2027;

INSERT INTO sys_menu
SELECT 2030, '门店设备', 2000, 3, 'device', NULL, '', '', 1, 0, 'M', '0', '0',
       '', 'monitor', 'admin', sysdate(), '', NULL, '门店设备管理目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2030);

INSERT INTO sys_menu
SELECT 2031, '标签打印机列表', 2030, 1, 'label-printer', 'admin/device/labelPrinter/index', '', '', 1, 0, 'C', '0', '0',
       'admin:label-printer:list', 'monitor', 'admin', sysdate(), '', NULL, '门店标签打印机管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2031);

INSERT INTO sys_menu
SELECT 2032, '标签打印机查询', 2031, 1, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:label-printer:query', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2032);

INSERT INTO sys_menu
SELECT 2033, '标签打印机新增', 2031, 2, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:label-printer:add', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2033);

INSERT INTO sys_menu
SELECT 2034, '标签打印机修改', 2031, 3, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:label-printer:edit', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2034);

INSERT INTO sys_menu
SELECT 2035, '标签打印机删除', 2031, 4, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:label-printer:remove', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2035);

INSERT INTO sys_menu
SELECT 2036, '查询打印机状态', 2031, 5, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:label-printer:status', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2036);

INSERT INTO sys_menu
SELECT 2037, '标签测试打印', 2031, 6, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:label-printer:test', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2037);

UPDATE sys_menu SET menu_name = '门店设备', icon = 'monitor', remark = '门店设备管理目录' WHERE menu_id = 2030;
UPDATE sys_menu SET menu_name = '标签打印机列表', component = 'admin/device/labelPrinter/index', icon = 'monitor', perms = 'admin:label-printer:list', remark = '门店标签打印机管理菜单' WHERE menu_id = 2031;
UPDATE sys_menu SET menu_name = '标签打印机查询', perms = 'admin:label-printer:query' WHERE menu_id = 2032;
UPDATE sys_menu SET menu_name = '标签打印机新增', perms = 'admin:label-printer:add' WHERE menu_id = 2033;
UPDATE sys_menu SET menu_name = '标签打印机修改', perms = 'admin:label-printer:edit' WHERE menu_id = 2034;
UPDATE sys_menu SET menu_name = '标签打印机删除', perms = 'admin:label-printer:remove' WHERE menu_id = 2035;
UPDATE sys_menu SET menu_name = '查询打印机状态', perms = 'admin:label-printer:status' WHERE menu_id = 2036;
UPDATE sys_menu SET menu_name = '标签测试打印', perms = 'admin:label-printer:test' WHERE menu_id = 2037;
