-- ============================================
-- P-Card Platform Database Initialization Script
-- ============================================
-- 完整的数据库初始化脚本，包含所有表结构和初始数据
-- 适用于从零开始部署项目
-- 
-- 使用方法:
--   mysql -u root -p < init.sql
-- 或在 MySQL 客户端中执行:
--   source /path/to/init.sql
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `p_card_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `p_card_db`;

-- ============================================
-- 用户表 (users)
-- 存储用户账户信息，密码使用 BCrypt 加密
-- ============================================
CREATE TABLE IF NOT EXISTS `users` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(100) NOT NULL UNIQUE COMMENT '用户名（唯一）',
    `nickname` VARCHAR(100) DEFAULT NULL COMMENT '用户昵称（用于显示）',
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '用户角色: user, admin',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '账户状态: active, banned',
    PRIMARY KEY (`id`),
    INDEX `idx_username` (`username`),
    INDEX `idx_nickname` (`nickname`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账户表';

-- ============================================
-- 名片表 (cards)
-- 存储用户创建的电子名片信息
-- ============================================
CREATE TABLE IF NOT EXISTS `cards` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` INT UNSIGNED NOT NULL COMMENT '所属用户ID',
    `producer_name` VARCHAR(255) COMMENT '制作人名称',
    `region` VARCHAR(100) COMMENT '地区',
    `idol_name` VARCHAR(255) COMMENT '偶像名称',
    `card_front_path` VARCHAR(500) COMMENT '名片正面图片路径',
    `card_back_path` VARCHAR(500) COMMENT '名片背面图片路径',
    `image_orientation` VARCHAR(20) DEFAULT 'HORIZONTAL' COMMENT '图片方向: HORIZONTAL(横版), VERTICAL(竖版)',
    `unique_link_id` VARCHAR(100) NOT NULL UNIQUE COMMENT '唯一分享链接ID',
    `custom_sns` TEXT COMMENT '自定义社交媒体链接(JSON格式)',
    `visibility` VARCHAR(20) DEFAULT 'PUBLIC' COMMENT '可见性: PUBLIC(公开), PRIVATE(私密)',
    `share_token` VARCHAR(255) DEFAULT NULL COMMENT '分享令牌',
    `short_code` VARCHAR(100) DEFAULT NULL COMMENT '短链接代码',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_unique_link_id` (`unique_link_id`),
    INDEX `idx_short_code` (`short_code`),
    INDEX `idx_visibility` (`visibility`),
    CONSTRAINT `fk_cards_user` FOREIGN KEY (`user_id`) 
        REFERENCES `users`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子名片表';

-- ============================================
-- 评论表 (comments)
-- 存储名片的评论和回复（支持嵌套回复）
-- ============================================
CREATE TABLE IF NOT EXISTS `comments` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `card_id` INT UNSIGNED NOT NULL COMMENT '所属名片ID',
    `user_id` INT UNSIGNED NOT NULL COMMENT '评论用户ID',
    `username` VARCHAR(100) COMMENT '评论用户名（冗余字段，提高查询性能）',
    `nickname` VARCHAR(100) COMMENT '评论用户昵称（冗余字段，提高查询性能）',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `parent_id` INT UNSIGNED DEFAULT NULL COMMENT '父评论ID，NULL表示顶级评论',
    `reply_to_username` VARCHAR(100) DEFAULT NULL COMMENT '回复的目标用户名',
    `reply_to_nickname` VARCHAR(100) DEFAULT NULL COMMENT '回复的目标用户昵称',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_card_id` (`card_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_created_at` (`created_at`),
    CONSTRAINT `fk_comments_card` FOREIGN KEY (`card_id`) 
        REFERENCES `cards`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) 
        REFERENCES `users`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_comments_parent` FOREIGN KEY (`parent_id`) 
        REFERENCES `comments`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表（支持嵌套回复）';

-- ============================================
-- 初始数据
-- ============================================

-- 插入默认管理员账户
-- 用户名: admin
-- 昵称: 管理员
-- 密码: admin
-- 密码哈希: $2a$12$Qk7CdOYDFB5pmod4sgAuru.QX8.keehGMMLpyIJ6nzUoQViE6mPQq
-- ⚠️ 重要提示: 首次登录后请立即修改管理员密码！
INSERT INTO `users` (`username`, `nickname`, `password`, `role`, `status`) VALUES
('admin', '管理员', '$2a$12$Qk7CdOYDFB5pmod4sgAuru.QX8.keehGMMLpyIJ6nzUoQViE6mPQq', 'admin', 'active')
ON DUPLICATE KEY UPDATE `username` = `username`; -- 避免重复插入

-- ============================================
-- 数据库初始化完成
-- ============================================
SELECT '✓ 数据库初始化成功！' AS Status,
       '数据库名称: p_card_db' AS Info1,
       '默认管理员: admin / admin' AS Info2,
       '⚠️ 请立即修改默认密码！' AS Warning;

-- 显示创建的表
SHOW TABLES;
