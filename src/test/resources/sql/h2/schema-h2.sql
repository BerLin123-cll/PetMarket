-- ============================================
-- H2 测试库建表脚本（MySQL 兼容模式）
-- 与 src/main/resources/sql/pet_market.sql 的表结构保持一致，
-- 但去掉了 MySQL 专有的 ENGINE / CHARSET / COMMENT 语法，便于 H2 执行。
-- ============================================

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50),
    `avatar` VARCHAR(255),
    `phone` VARCHAR(20),
    `gender` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `status` TINYINT DEFAULT 1,
    PRIMARY KEY (`id`),
    UNIQUE (`username`)
);

DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE (`username`)
);

DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `icon` VARCHAR(255),
    `sort_order` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(200) NOT NULL,
    `category_id` BIGINT NOT NULL,
    `main_image` VARCHAR(255),
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `stock` INT NOT NULL DEFAULT 0,
    `description` TEXT,
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE (`user_id`, `product_id`)
);

DROP TABLE IF EXISTS `address`;
CREATE TABLE `address` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `receiver_name` VARCHAR(50) NOT NULL,
    `phone` VARCHAR(20) NOT NULL,
    `detail_address` VARCHAR(500) NOT NULL,
    `is_default` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_no` VARCHAR(64) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `receiver_name` VARCHAR(50) NOT NULL,
    `receiver_phone` VARCHAR(20) NOT NULL,
    `receiver_address` VARCHAR(500) NOT NULL,
    `order_status` TINYINT DEFAULT 0,
    `pay_status` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `pay_time` DATETIME,
    `pay_expire_time` DATETIME,
    PRIMARY KEY (`id`),
    UNIQUE (`order_no`)
);

DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `product_name` VARCHAR(200) NOT NULL,
    `product_image` VARCHAR(255),
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `quantity` INT NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `upload_file`;
CREATE TABLE `upload_file` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `biz_type` VARCHAR(32) NOT NULL DEFAULT 'other',
    `original_name` VARCHAR(255),
    `stored_name` VARCHAR(128) NOT NULL,
    `relative_path` VARCHAR(255) NOT NULL,
    `url` VARCHAR(500) NOT NULL,
    `file_size` BIGINT,
    `content_type` VARCHAR(128),
    `uploader_id` BIGINT,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

-- ---------- 测试基础数据 ----------
INSERT INTO `admin` (`username`, `password`, `nickname`) VALUES
('admin', '$2a$10$9HXI8Qf36pc4jWQpTTugU.pdllKFpmP7N38aHQ4wHfQ4wygxAKWr2', '超级管理员');

INSERT INTO `category` (`name`, `sort_order`, `status`) VALUES ('狗狗用品', 1, 1);

INSERT INTO `product` (`name`, `category_id`, `main_image`, `price`, `stock`, `description`, `status`) VALUES
('测试狗粮', 1, '/images/product/dog-food.jpg', 100.00, 100, '用于测试的商品', 1);

INSERT INTO `user` (`username`, `password`, `nickname`, `phone`, `gender`) VALUES
('testuser', '$2a$10$PDe2DACKzG/D23wC06dPuuiogmBmglTqOZXFftL1xj.99ZjsduCCy', '测试用户', '13800138000', 1);

INSERT INTO `address` (`user_id`, `receiver_name`, `phone`, `detail_address`, `is_default`) VALUES
(1, '张三', '13800138000', '北京市朝阳区测试路1号', 1);
