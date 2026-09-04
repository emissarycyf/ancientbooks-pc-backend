CREATE DATABASE IF NOT EXISTS ancient_books_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ancient_books_db;

-- 对话历史表
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

-- 古籍元数据表（扩展用）
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
