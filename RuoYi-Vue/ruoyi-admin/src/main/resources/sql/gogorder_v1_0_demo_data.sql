-- gogorder V1.0 demo shops and products
-- Safe to execute repeatedly on MySQL 8.0.

SET NAMES utf8mb4;

INSERT INTO shop (
  shop_code, name, phone, province, city, district, address, longitude, latitude,
  open_time, close_time, status, notice, pack_fee, sort_order, del_flag
) VALUES
  ('DEMO-NN-001', '茶序万象城店', '07715550101', '广西', '南宁市', '青秀区', '民族大道136号万象城B1层', 108.3932600, 22.8125300, '09:30:00', '22:30:00', 1, '欢迎光临，工作日午间可能需要等待约10分钟。', 100, 10, 0),
  ('DEMO-NN-002', '茶序朝阳广场店', '07715550102', '广西', '南宁市', '兴宁区', '朝阳路38号朝阳广场1层', 108.3208200, 22.8294200, '10:00:00', '23:00:00', 1, '本店支持预订单，到店即取。', 100, 20, 0),
  ('DEMO-NN-003', '茶序大学城店', '07715550103', '广西', '南宁市', '西乡塘区', '大学东路100号美食街A12铺', 108.2910800, 22.8381900, '10:30:00', '01:00:00', 2, '当前暂停即时单，可预约稍后取餐。', 50, 30, 0),
  ('DEMO-NN-004', '茶序江南盛天地店', '07715550104', '广西', '南宁市', '江南区', '壮锦大道27号盛天地1层', 108.2756300, 22.7876900, '11:00:00', '22:00:00', 0, '门店今日休息，仍可预约明日订单。', 100, 40, 0)
ON DUPLICATE KEY UPDATE
  name = VALUES(name), phone = VALUES(phone), province = VALUES(province), city = VALUES(city),
  district = VALUES(district), address = VALUES(address), longitude = VALUES(longitude),
  latitude = VALUES(latitude), open_time = VALUES(open_time), close_time = VALUES(close_time),
  status = VALUES(status), notice = VALUES(notice), pack_fee = VALUES(pack_fee),
  sort_order = VALUES(sort_order), del_flag = 0;

INSERT INTO category (name, sort_order, status)
SELECT '人气推荐', 10, 1 WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '人气推荐');
INSERT INTO category (name, sort_order, status)
SELECT '经典奶茶', 20, 1 WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '经典奶茶');
INSERT INTO category (name, sort_order, status)
SELECT '鲜果茶', 30, 1 WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '鲜果茶');
INSERT INTO category (name, sort_order, status)
SELECT '纯茶', 40, 1 WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '纯茶');
INSERT INTO category (name, sort_order, status)
SELECT '咖啡', 50, 1 WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '咖啡');
INSERT INTO category (name, sort_order, status)
SELECT '小料甜品', 60, 1 WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '小料甜品');

INSERT INTO spec_template (name, type, is_required, min_select, max_select, sort_order, status)
SELECT '杯型', 1, 1, 1, 1, 10, 1 WHERE NOT EXISTS (SELECT 1 FROM spec_template WHERE name = '杯型');
INSERT INTO spec_template (name, type, is_required, min_select, max_select, sort_order, status)
SELECT '温度', 1, 1, 1, 1, 20, 1 WHERE NOT EXISTS (SELECT 1 FROM spec_template WHERE name = '温度');
INSERT INTO spec_template (name, type, is_required, min_select, max_select, sort_order, status)
SELECT '甜度', 1, 1, 1, 1, 30, 1 WHERE NOT EXISTS (SELECT 1 FROM spec_template WHERE name = '甜度');
INSERT INTO spec_template (name, type, is_required, min_select, max_select, sort_order, status)
SELECT '加料', 2, 0, 0, 3, 40, 1 WHERE NOT EXISTS (SELECT 1 FROM spec_template WHERE name = '加料');

SET @cup = (SELECT id FROM spec_template WHERE name = '杯型' ORDER BY id LIMIT 1);
SET @temperature = (SELECT id FROM spec_template WHERE name = '温度' ORDER BY id LIMIT 1);
SET @sweetness = (SELECT id FROM spec_template WHERE name = '甜度' ORDER BY id LIMIT 1);
SET @topping = (SELECT id FROM spec_template WHERE name = '加料' ORDER BY id LIMIT 1);

INSERT INTO spec_option (option_id, template_id, label, price_add, is_default, sort_order, status) VALUES
  ('demo-cup-medium', @cup, '中杯', 0, 1, 10, 1),
  ('demo-cup-large', @cup, '大杯', 300, 0, 20, 1),
  ('demo-temp-ice', @temperature, '正常冰', 0, 1, 10, 1),
  ('demo-temp-less-ice', @temperature, '少冰', 0, 0, 20, 1),
  ('demo-temp-no-ice', @temperature, '去冰', 0, 0, 30, 1),
  ('demo-temp-hot', @temperature, '热饮', 0, 0, 40, 1),
  ('demo-sugar-full', @sweetness, '标准甜', 0, 1, 10, 1),
  ('demo-sugar-70', @sweetness, '七分甜', 0, 0, 20, 1),
  ('demo-sugar-50', @sweetness, '五分甜', 0, 0, 30, 1),
  ('demo-sugar-30', @sweetness, '三分甜', 0, 0, 40, 1),
  ('demo-sugar-none', @sweetness, '不另外加糖', 0, 0, 50, 1),
  ('demo-top-pearl', @topping, '珍珠', 100, 0, 10, 1),
  ('demo-top-coconut', @topping, '椰果', 100, 0, 20, 1),
  ('demo-top-pudding', @topping, '布丁', 200, 0, 30, 1),
  ('demo-top-cheese', @topping, '芝士奶盖', 300, 0, 40, 1)
ON DUPLICATE KEY UPDATE
  template_id = VALUES(template_id), label = VALUES(label), price_add = VALUES(price_add),
  is_default = VALUES(is_default), sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO product (name, image, images, description, base_price, spec_template_ids, tags, status)
SELECT '招牌珍珠奶茶', '', JSON_ARRAY(), '醇香红茶与鲜奶融合，搭配Q弹珍珠。', 1600,
       JSON_ARRAY(@cup, @temperature, @sweetness, @topping), JSON_ARRAY('招牌', '热销'), 1
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '招牌珍珠奶茶');
INSERT INTO product (name, image, images, description, base_price, spec_template_ids, tags, status)
SELECT '黑糖波波牛乳', '', JSON_ARRAY(), '黑糖慢熬波波与浓醇牛乳，口感丰富。', 1900,
       JSON_ARRAY(@cup, @temperature, @sweetness, @topping), JSON_ARRAY('人气', '牛乳'), 1
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '黑糖波波牛乳');
INSERT INTO product (name, image, images, description, base_price, spec_template_ids, tags, status)
SELECT '芝士葡萄', '', JSON_ARRAY(), '整颗葡萄果肉搭配清香绿茶与芝士奶盖。', 2200,
       JSON_ARRAY(@cup, @temperature, @sweetness), JSON_ARRAY('鲜果', '推荐'), 1
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '芝士葡萄');
INSERT INTO product (name, image, images, description, base_price, spec_template_ids, tags, status)
SELECT '满杯红柚', '', JSON_ARRAY(), '红柚果粒与茉莉绿茶，酸甜清爽。', 1800,
       JSON_ARRAY(@cup, @temperature, @sweetness), JSON_ARRAY('鲜果', '清爽'), 1
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '满杯红柚');
INSERT INTO product (name, image, images, description, base_price, spec_template_ids, tags, status)
SELECT '百香双响炮', '', JSON_ARRAY(), '百香果、珍珠与椰果的经典组合。', 1700,
       JSON_ARRAY(@cup, @temperature, @sweetness, @topping), JSON_ARRAY('经典', '酸甜'), 1
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '百香双响炮');
INSERT INTO product (name, image, images, description, base_price, spec_template_ids, tags, status)
SELECT '茉莉绿茶', '', JSON_ARRAY(), '清新茉莉香气，回甘爽口。', 900,
       JSON_ARRAY(@cup, @temperature, @sweetness), JSON_ARRAY('纯茶'), 1
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '茉莉绿茶');
INSERT INTO product (name, image, images, description, base_price, spec_template_ids, tags, status)
SELECT '金桂乌龙', '', JSON_ARRAY(), '桂花香与乌龙茶韵交织。', 1100,
       JSON_ARRAY(@cup, @temperature, @sweetness), JSON_ARRAY('纯茶', '无奶'), 1
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '金桂乌龙');
INSERT INTO product (name, image, images, description, base_price, spec_template_ids, tags, status)
SELECT '生椰拿铁', '', JSON_ARRAY(), '椰乳与浓缩咖啡融合，顺滑不腻。', 2000,
       JSON_ARRAY(@cup, @temperature, @sweetness), JSON_ARRAY('咖啡', '人气'), 1
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '生椰拿铁');
INSERT INTO product (name, image, images, description, base_price, spec_template_ids, tags, status)
SELECT '桂花燕麦拿铁', '', JSON_ARRAY(), '燕麦奶拿铁点缀桂花香。', 2300,
       JSON_ARRAY(@cup, @temperature, @sweetness), JSON_ARRAY('咖啡', '燕麦奶'), 1
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '桂花燕麦拿铁');
INSERT INTO product (name, image, images, description, base_price, spec_template_ids, tags, status)
SELECT '芋泥啵啵奶茶', '', JSON_ARRAY(), '绵密芋泥搭配啵啵与奶茶。', 2100,
       JSON_ARRAY(@cup, @temperature, @sweetness, @topping), JSON_ARRAY('芋泥', '热销'), 1
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '芋泥啵啵奶茶');
INSERT INTO product (name, image, images, description, base_price, spec_template_ids, tags, status)
SELECT '杨枝甘露', '', JSON_ARRAY(), '芒果、西柚与椰乳的经典甜品饮。', 2400,
       JSON_ARRAY(@cup, @temperature, @sweetness), JSON_ARRAY('甜品', '鲜果'), 1
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '杨枝甘露');
INSERT INTO product (name, image, images, description, base_price, spec_template_ids, tags, status)
SELECT '焦糖布丁奶茶', '', JSON_ARRAY(), '焦糖风味奶茶搭配嫩滑布丁。', 1900,
       JSON_ARRAY(@cup, @temperature, @sweetness, @topping), JSON_ARRAY('奶茶', '布丁'), 1
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '焦糖布丁奶茶');

SET @cat_popular = (SELECT id FROM category WHERE name = '人气推荐' ORDER BY id LIMIT 1);
SET @cat_milk_tea = (SELECT id FROM category WHERE name = '经典奶茶' ORDER BY id LIMIT 1);
SET @cat_fruit = (SELECT id FROM category WHERE name = '鲜果茶' ORDER BY id LIMIT 1);
SET @cat_tea = (SELECT id FROM category WHERE name = '纯茶' ORDER BY id LIMIT 1);
SET @cat_coffee = (SELECT id FROM category WHERE name = '咖啡' ORDER BY id LIMIT 1);
SET @cat_dessert = (SELECT id FROM category WHERE name = '小料甜品' ORDER BY id LIMIT 1);

INSERT IGNORE INTO product_category (product_id, category_id, sort_order)
SELECT id, @cat_popular, 10 FROM product WHERE name IN ('招牌珍珠奶茶', '黑糖波波牛乳', '芝士葡萄', '生椰拿铁', '芋泥啵啵奶茶');
INSERT IGNORE INTO product_category (product_id, category_id, sort_order)
SELECT id, @cat_milk_tea, 20 FROM product WHERE name IN ('招牌珍珠奶茶', '黑糖波波牛乳', '百香双响炮', '芋泥啵啵奶茶', '焦糖布丁奶茶');
INSERT IGNORE INTO product_category (product_id, category_id, sort_order)
SELECT id, @cat_fruit, 30 FROM product WHERE name IN ('芝士葡萄', '满杯红柚', '百香双响炮', '杨枝甘露');
INSERT IGNORE INTO product_category (product_id, category_id, sort_order)
SELECT id, @cat_tea, 40 FROM product WHERE name IN ('茉莉绿茶', '金桂乌龙');
INSERT IGNORE INTO product_category (product_id, category_id, sort_order)
SELECT id, @cat_coffee, 50 FROM product WHERE name IN ('生椰拿铁', '桂花燕麦拿铁');
INSERT IGNORE INTO product_category (product_id, category_id, sort_order)
SELECT id, @cat_dessert, 60 FROM product WHERE name IN ('黑糖波波牛乳', '芋泥啵啵奶茶', '杨枝甘露', '焦糖布丁奶茶');

SET @shop_1 = (SELECT id FROM shop WHERE shop_code = 'DEMO-NN-001');
SET @shop_2 = (SELECT id FROM shop WHERE shop_code = 'DEMO-NN-002');
SET @shop_3 = (SELECT id FROM shop WHERE shop_code = 'DEMO-NN-003');
SET @shop_4 = (SELECT id FROM shop WHERE shop_code = 'DEMO-NN-004');

INSERT IGNORE INTO shop_product (shop_id, product_id, price, status, stock, sort_order)
SELECT @shop_1, id, NULL, 1, -1, id FROM product;
INSERT IGNORE INTO shop_product (shop_id, product_id, price, status, stock, sort_order)
SELECT @shop_2, id, CASE WHEN name IN ('生椰拿铁', '桂花燕麦拿铁') THEN base_price + 200 ELSE NULL END, 1, -1, id
FROM product;
INSERT IGNORE INTO shop_product (shop_id, product_id, price, status, stock, sort_order)
SELECT @shop_3, id, NULL, 1, CASE WHEN name IN ('芝士葡萄', '杨枝甘露') THEN 30 ELSE 80 END, id
FROM product WHERE name NOT IN ('金桂乌龙');
INSERT IGNORE INTO shop_product (shop_id, product_id, price, status, stock, sort_order)
SELECT @shop_4, id, NULL, CASE WHEN name IN ('满杯红柚', '芝士葡萄') THEN 0 ELSE 1 END, -1, id
FROM product WHERE name IN ('招牌珍珠奶茶', '黑糖波波牛乳', '满杯红柚', '芝士葡萄', '茉莉绿茶', '生椰拿铁');
