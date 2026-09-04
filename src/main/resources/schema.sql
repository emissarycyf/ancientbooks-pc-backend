-- =============================================
-- 古籍分析 Agent 数据库初始化脚本
-- 适用于 MySQL 8.0+
-- =============================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS ancient_books_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ancient_books_db;

-- 2. 创建对话历史表
CREATE TABLE chat_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id VARCHAR(64) DEFAULT 'anonymous' COMMENT '用户标识',
    conversation_id VARCHAR(128) COMMENT 'Coze 会话ID',
    user_query TEXT COMMENT '用户提问',
    ai_reply LONGTEXT COMMENT 'Agent 返回结果',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='古籍对话历史表';

-- 3. 创建古籍元数据表
CREATE TABLE ancient_book (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL COMMENT '书名',
    author VARCHAR(128) COMMENT '作者',
    dynasty VARCHAR(64) COMMENT '朝代',
    category VARCHAR(64) COMMENT '分类：经史子集',
    content TEXT COMMENT '内容摘要',
    file_path VARCHAR(512) COMMENT '文件路径',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='古籍元数据表';

-- 4. 创建用户表
CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码（BCrypt 加密）',
    email VARCHAR(128) COMMENT '邮箱',
    role VARCHAR(32) DEFAULT 'USER' COMMENT '角色：ADMIN、USER',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 5. 插入默认管理员账户（密码：admin123，BCrypt 加密后）
INSERT INTO `user` (username, password, email, role, status)
VALUES ('admin', '$2a$10$YQYxhYqLq5YqLq5YqLq5YOHLq5YqLq5YqLq5YqLq5YqLq5YqLq5Yq', 'admin@ancientbooks.com', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE id=id;

-- 6. 验证数据
SELECT 'Database initialized successfully' AS status;
SELECT COUNT(*) AS user_count FROM user;
SELECT username, email, role, status FROM user WHERE username = 'admin';
