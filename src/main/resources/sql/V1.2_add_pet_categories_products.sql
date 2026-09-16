-- ============================================
-- PetMarket 增量脚本 V1.2
-- 用途：给「已经建好并在用」的库补充更多动物商品分类与商品数据
--       （全新建库无需执行本脚本，直接跑 pet_market.sql 即可）
--
-- 特点：幂等可重复执行
--   1. 分类按「名称」判重，已存在则跳过；
--   2. 商品按「名称」判重，且用子查询按分类名关联 category_id，
--      不依赖自增ID，避免不同环境ID错位；
--   3. main_image 一律留空(NULL)，图片由管理员通过后台上传后再补充。
--
-- 执行方式（PowerShell，注意用 --default-character-set=utf8mb4 防止中文乱码）：
--   mysql -uroot -p --default-character-set=utf8mb4 pet_market < src/main/resources/sql/V1.2_add_pet_categories_products.sql
-- ============================================

USE pet_market;

-- ---------- 1. 新增分类 ----------
INSERT INTO `category` (`name`, `icon`, `sort_order`, `status`)
SELECT '小宠用品', '/images/category/small-pet.png', 5, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '小宠用品');

INSERT INTO `category` (`name`, `icon`, `sort_order`, `status`)
SELECT '爬宠用品', '/images/category/reptile.png', 6, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '爬宠用品');

INSERT INTO `category` (`name`, `icon`, `sort_order`, `status`)
SELECT '宠物医疗保健', '/images/category/health.png', 7, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '宠物医疗保健');

-- ---------- 2. 新增商品 ----------
-- 狗狗用品
INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '狗狗牵引绳反光胸背带', c.`id`, NULL, 39.00, 110, '反光设计夜间遛狗更安全，可调节胸背带', 1
FROM `category` c WHERE c.`name` = '狗狗用品'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '狗狗牵引绳反光胸背带');

INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '宠物狗窝四季通用可拆洗', c.`id`, NULL, 89.00, 55, '柔软舒适，外套可拆洗，四季通用', 1
FROM `category` c WHERE c.`name` = '狗狗用品'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '宠物狗窝四季通用可拆洗');

-- 猫咪用品
INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '猫抓板瓦楞纸耐磨', c.`id`, NULL, 25.00, 140, '高密度瓦楞纸，耐抓耐磨，保护家具', 1
FROM `category` c WHERE c.`name` = '猫咪用品'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '猫抓板瓦楞纸耐磨');

INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '猫条营养零食鸡肉味', c.`id`, NULL, 19.90, 300, '鸡肉配方，补水又营养，互动喂食佳品', 1
FROM `category` c WHERE c.`name` = '猫咪用品'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '猫条营养零食鸡肉味');

-- 水族宠物
INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '鱼缸增氧泵静音气泵', c.`id`, NULL, 55.00, 80, '低噪静音，出氧稳定，适合中小型鱼缸', 1
FROM `category` c WHERE c.`name` = '水族宠物'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '鱼缸增氧泵静音气泵');

-- 鸟类宠物
INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '鸟笼金属大号鹦鹉笼', c.`id`, NULL, 168.00, 35, '加厚金属材质，大空间，带食盒栖木', 1
FROM `category` c WHERE c.`name` = '鸟类宠物'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '鸟笼金属大号鹦鹉笼');

-- 小宠用品（仓鼠/兔子/龙猫等）
INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '仓鼠跑轮静音转轮', c.`id`, NULL, 19.90, 150, '静音轴承，运动不扰眠，适合各类小宠', 1
FROM `category` c WHERE c.`name` = '小宠用品'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '仓鼠跑轮静音转轮');

INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '提摩西草成兔粮2.5kg', c.`id`, NULL, 58.00, 90, '高纤维提摩西草配方，帮助磨牙助消化', 1
FROM `category` c WHERE c.`name` = '小宠用品'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '提摩西草成兔粮2.5kg');

INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '龙猫浴沙除臭洗澡沙1kg', c.`id`, NULL, 22.00, 120, '细腻浴沙，去油除臭，龙猫洗澡必备', 1
FROM `category` c WHERE c.`name` = '小宠用品'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '龙猫浴沙除臭洗澡沙1kg');

INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '小宠挂式饮水器水壶', c.`id`, NULL, 15.50, 200, '滚珠出水不漏水，悬挂固定方便卫生', 1
FROM `category` c WHERE c.`name` = '小宠用品'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '小宠挂式饮水器水壶');

-- 爬宠用品（龟/蜥蜴等）
INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '乌龟晒背灯UVB加热灯套装', c.`id`, NULL, 79.00, 60, 'UVB补钙晒背，含灯架灯泡，模拟日光', 1
FROM `category` c WHERE c.`name` = '爬宠用品'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '乌龟晒背灯UVB加热灯套装');

INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '爬虫饲养箱通风整理箱', c.`id`, NULL, 128.00, 40, '透明观察窗，通风透气，便于清洁', 1
FROM `category` c WHERE c.`name` = '爬宠用品'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '爬虫饲养箱通风整理箱');

INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '蜥蜴钙粉维生素营养剂', c.`id`, NULL, 32.00, 100, '补充钙质与维生素，预防爬宠软骨病', 1
FROM `category` c WHERE c.`name` = '爬宠用品'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '蜥蜴钙粉维生素营养剂');

INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '爬虫加热垫温控恒温垫', c.`id`, NULL, 45.00, 75, '恒温发热，冬日保温，爬宠过冬必备', 1
FROM `category` c WHERE c.`name` = '爬宠用品'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '爬虫加热垫温控恒温垫');

-- 宠物医疗保健
INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '宠物体内外驱虫药滴剂', c.`id`, NULL, 68.00, 130, '体内外同驱，滴剂使用方便，猫狗通用', 1
FROM `category` c WHERE c.`name` = '宠物医疗保健'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '宠物体内外驱虫药滴剂');

INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '猫狗通用益生菌调理肠胃', c.`id`, NULL, 49.00, 160, '补充活性益生菌，改善软便调理肠胃', 1
FROM `category` c WHERE c.`name` = '宠物医疗保健'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '猫狗通用益生菌调理肠胃');

INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`)
SELECT '宠物关节保健软骨素片', c.`id`, NULL, 98.00, 70, '含氨基葡萄糖与软骨素，呵护关节健康', 1
FROM `category` c WHERE c.`name` = '宠物医疗保健'
AND NOT EXISTS (SELECT 1 FROM `product` p WHERE p.`name` = '宠物关节保健软骨素片');
