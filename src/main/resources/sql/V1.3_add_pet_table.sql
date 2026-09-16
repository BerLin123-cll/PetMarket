-- ============================================
-- PetMarket 增量脚本 V1.3
-- 用途：给「已经在用」的库新增「宠物档案表 pet」（P2 功能）
--       （全新建库无需执行本脚本，直接跑 pet_market.sql 即可）
--
-- 特点：幂等可重复执行（CREATE TABLE IF NOT EXISTS）
--
-- 执行方式（PowerShell，注意用 --default-character-set=utf8mb4 防止中文乱码）：
--   mysql -uroot -p --default-character-set=utf8mb4 pet_market -e "source src/main/resources/sql/V1.3_add_pet_table.sql"
-- ============================================

USE pet_market;

CREATE TABLE IF NOT EXISTS `pet` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '宠物ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '主人用户ID',
    `name` VARCHAR(50) NOT NULL COMMENT '宠物昵称',
    `species` TINYINT(2) NOT NULL COMMENT '物种：1-狗，2-猫，3-小宠，4-爬宠，5-鸟，6-鱼',
    `breed` VARCHAR(50) DEFAULT NULL COMMENT '品种',
    `gender` TINYINT(1) DEFAULT 0 COMMENT '性别：0-未知，1-公，2-母',
    `birthday` DATE DEFAULT NULL COMMENT '生日',
    `weight` DECIMAL(5,2) DEFAULT NULL COMMENT '体重(kg)',
    `allergy_history` VARCHAR(500) DEFAULT NULL COMMENT '过敏史（逗号/顿号分隔的关键词，如：鸡肉,牛肉）',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '宠物头像URL',
    `last_deworm_date` DATE DEFAULT NULL COMMENT '上次驱虫日期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物档案表';
