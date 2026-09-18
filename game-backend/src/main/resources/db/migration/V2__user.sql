-- 用户表（W2 用户体系: 注册/登录，替代前端匿名 UUID）
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username      VARCHAR(32)  NOT NULL COMMENT '用户名(唯一)',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';

-- W2 决策: 老匿名数据（前端 localStorage UUID）无法归属新账号体系，直接放弃（2026-09-18 与用户确认）
DELETE FROM completed_games;
