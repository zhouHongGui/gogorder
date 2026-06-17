-- gogorder V1.0 printer menu patch
-- 本脚本用于在已有数据库上新增"门店设备 / 标签打印机列表"菜单及按钮权限。

-- 统一字符集
SET NAMES utf8mb4;

-- 目录菜单：门店设备（menu_id=2030），挂在门店管理目录(2000)下
-- 采用 INSERT ... WHERE NOT EXISTS 实现幂等插入，避免重复执行报错
INSERT INTO sys_menu
SELECT 2030, '门店设备', 2000, 3, 'device', NULL, '', '', 1, 0, 'M', '0', '0',
       '', 'monitor', 'admin', sysdate(), '', NULL, '门店设备管理目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2030);

-- 菜单页面：标签打印机列表（menu_id=2031），组件路径 admin/device/labelPrinter/index
INSERT INTO sys_menu
SELECT 2031, '标签打印机列表', 2030, 1, 'label-printer', 'admin/device/labelPrinter/index', '', '', 1, 0, 'C', '0', '0',
       'admin:label-printer:list', 'monitor', 'admin', sysdate(), '', NULL, '门店标签打印机管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2031);

-- 按钮权限：查询（menu_id=2032，权限标识 admin:label-printer:query）
INSERT INTO sys_menu
SELECT 2032, '标签打印机查询', 2031, 1, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:label-printer:query', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2032);

-- 按钮权限：新增（menu_id=2033，权限标识 admin:label-printer:add）
INSERT INTO sys_menu
SELECT 2033, '标签打印机新增', 2031, 2, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:label-printer:add', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2033);

-- 按钮权限：修改（menu_id=2034，权限标识 admin:label-printer:edit）
INSERT INTO sys_menu
SELECT 2034, '标签打印机修改', 2031, 3, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:label-printer:edit', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2034);

-- 按钮权限：删除（menu_id=2035，权限标识 admin:label-printer:remove）
INSERT INTO sys_menu
SELECT 2035, '标签打印机删除', 2031, 4, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:label-printer:remove', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2035);

-- 按钮权限：查询打印机状态（menu_id=2036，权限标识 admin:label-printer:status）
INSERT INTO sys_menu
SELECT 2036, '查询打印机状态', 2031, 5, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:label-printer:status', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2036);

-- 按钮权限：标签测试打印（menu_id=2037，权限标识 admin:label-printer:test）
INSERT INTO sys_menu
SELECT 2037, '标签测试打印', 2031, 6, '#', '', '', '', 1, 0, 'F', '0', '0',
       'admin:label-printer:test', '#', 'admin', sysdate(), '', NULL, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2037);

-- 以下 UPDATE 用于修正/对齐已存在记录的名称、组件、图标、权限和备注，确保多次执行结果一致
UPDATE sys_menu SET menu_name = '门店设备', icon = 'monitor', remark = '门店设备管理目录' WHERE menu_id = 2030; -- 目录
UPDATE sys_menu SET menu_name = '标签打印机列表', component = 'admin/device/labelPrinter/index', icon = 'monitor', perms = 'admin:label-printer:list', remark = '门店标签打印机管理菜单' WHERE menu_id = 2031; -- 列表页
UPDATE sys_menu SET menu_name = '标签打印机查询', perms = 'admin:label-printer:query' WHERE menu_id = 2032; -- 查询按钮
UPDATE sys_menu SET menu_name = '标签打印机新增', perms = 'admin:label-printer:add' WHERE menu_id = 2033; -- 新增按钮
UPDATE sys_menu SET menu_name = '标签打印机修改', perms = 'admin:label-printer:edit' WHERE menu_id = 2034; -- 修改按钮
UPDATE sys_menu SET menu_name = '标签打印机删除', perms = 'admin:label-printer:remove' WHERE menu_id = 2035; -- 删除按钮
UPDATE sys_menu SET menu_name = '查询打印机状态', perms = 'admin:label-printer:status' WHERE menu_id = 2036; -- 状态查询按钮
UPDATE sys_menu SET menu_name = '标签测试打印', perms = 'admin:label-printer:test' WHERE menu_id = 2037; -- 测试打印按钮
