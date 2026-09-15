-- ============================================
-- PetMarket 萌宠商城 - 数据库初始化脚本
-- 数据库名: pet_market
-- MySQL版本: 8.0+
-- ============================================

CREATE DATABASE IF NOT EXISTS pet_market DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE pet_market;

-- ============================================
-- 1. 用户表 user
-- ============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `gender` TINYINT(1) DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `status` TINYINT(1) DEFAULT 1 COMMENT '账号状态：0-禁用，1-启用',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. 管理员表 admin
-- ============================================
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    `username` VARCHAR(50) NOT NULL COMMENT '账号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- ============================================
-- 3. 商品分类表 category
-- ============================================
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标URL',
    `sort_order` INT(11) DEFAULT 0 COMMENT '排序值（越小越靠前）',
    `status` TINYINT(1) DEFAULT 1 COMMENT '上架状态：0-下架，1-上架',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ============================================
-- 4. 商品表 product
-- ============================================
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `category_id` BIGINT(20) NOT NULL COMMENT '分类ID',
    `main_image` VARCHAR(255) DEFAULT NULL COMMENT '主图URL',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '价格',
    `stock` INT(11) NOT NULL DEFAULT 0 COMMENT '库存数量',
    `description` TEXT COMMENT '详情描述',
    `status` TINYINT(1) DEFAULT 1 COMMENT '上架状态：0-下架，1-上架',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ============================================
-- 5. 购物车表 cart
-- ============================================
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `product_id` BIGINT(20) NOT NULL COMMENT '商品ID',
    `quantity` INT(11) NOT NULL DEFAULT 1 COMMENT '购买数量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ============================================
-- 6. 收货地址表 address
-- ============================================
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `detail_address` VARCHAR(500) NOT NULL COMMENT '详细地址',
    `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否默认地址：0-否，1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- ============================================
-- 7. 订单表 orders
-- ============================================
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货电话',
    `receiver_address` VARCHAR(500) NOT NULL COMMENT '收货地址',
    `order_status` TINYINT(2) DEFAULT 0 COMMENT '订单状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消',
    `pay_status` TINYINT(1) DEFAULT 0 COMMENT '支付状态：0-未支付，1-已支付',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `pay_expire_time` DATETIME DEFAULT NULL COMMENT '支付截止时间（超时未支付自动取消并回补库存）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`order_status`),
    KEY `idx_timeout` (`order_status`, `pay_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ============================================
-- 8. 订单项表 order_item
-- ============================================
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
    `order_id` BIGINT(20) NOT NULL COMMENT '订单ID',
    `product_id` BIGINT(20) NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称（快照）',
    `product_image` VARCHAR(255) DEFAULT NULL COMMENT '商品快照图',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '购买单价',
    `quantity` INT(11) NOT NULL DEFAULT 1 COMMENT '购买数量',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项表';

-- ============================================
-- 9. 上传文件记录表 upload_file
--    （V1.1 新增：本地图片上传，文件存磁盘，此处只存元信息）
-- ============================================
DROP TABLE IF EXISTS `upload_file`;
CREATE TABLE `upload_file` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `biz_type` VARCHAR(32) NOT NULL DEFAULT 'other' COMMENT '业务分类：product/avatar/category/other',
    `original_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    `stored_name` VARCHAR(128) NOT NULL COMMENT '服务端存储文件名（UUID）',
    `relative_path` VARCHAR(255) NOT NULL COMMENT '磁盘相对路径',
    `url` VARCHAR(500) NOT NULL COMMENT '对外访问地址',
    `file_size` BIGINT(20) DEFAULT NULL COMMENT '文件大小（字节）',
    `content_type` VARCHAR(128) DEFAULT NULL COMMENT 'MIME 类型',
    `uploader_id` BIGINT(20) DEFAULT NULL COMMENT '上传人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_biz_type` (`biz_type`),
    KEY `idx_uploader` (`uploader_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传文件记录表';

-- ============================================
-- 老库升级脚本（已上线过的库执行这一段即可，全新建库可跳过）
-- ============================================
-- ALTER TABLE `orders` ADD COLUMN `pay_expire_time` DATETIME DEFAULT NULL COMMENT '支付截止时间' AFTER `pay_time`;
-- ALTER TABLE `orders` ADD INDEX `idx_timeout` (`order_status`, `pay_status`, `create_time`);
-- 然后执行上面的 upload_file 建表语句

-- ============================================
-- 插入测试数据
-- ============================================

-- 插入管理员（密码：admin123，BCrypt加密）
INSERT INTO `admin` (`username`, `password`, `nickname`) VALUES
('admin', '$2a$10$9HXI8Qf36pc4jWQpTTugU.pdllKFpmP7N38aHQ4wHfQ4wygxAKWr2', '超级管理员');

-- 插入分类
INSERT INTO `category` (`name`, `icon`, `sort_order`, `status`) VALUES
('狗狗用品', '/images/category/dog.png', 1, 1),
('猫咪用品', '/images/category/cat.png', 2, 1),
('水族宠物', '/images/category/fish.png', 3, 1),
('鸟类宠物', '/images/category/bird.png', 4, 1);

-- 插入测试商品
INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`) VALUES
('皇家狗粮小型犬成犬粮2kg', 1, '/images/product/dog-food.jpg', 128.00, 100, '适合小型犬食用，营养均衡', 1),
('猫砂盆全封闭式防外溅', 2, '/images/product/cat-litter.jpg', 89.00, 50, '全封闭设计，有效防止猫砂外溅', 1),
('热带鱼饲料通用型100g', 3, '/images/product/fish-food.jpg', 25.00, 200, '适用于各种热带鱼', 1),
('鹦鹉玩具啃咬球套装', 4, '/images/product/bird-toy.jpg', 35.00, 80, '天然木质，安全无毒', 1);

-- 插入测试用户（密码：123456，BCrypt加密）
INSERT INTO `user` (`username`, `password`, `nickname`, `phone`, `gender`) VALUES
('testuser', '$2a$10$PDe2DACKzG/D23wC06dPuuiogmBmglTqOZXFftL1xj.99ZjsduCCy', '测试用户', '13800138000', 1);
