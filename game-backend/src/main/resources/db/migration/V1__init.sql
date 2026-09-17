-- 海龟汤已完成游戏记录表
CREATE TABLE IF NOT EXISTS completed_games (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      VARCHAR(64)  NOT NULL COMMENT '用户ID',
    room_id      VARCHAR(128) NOT NULL COMMENT '游戏会话ID(memoryId)，唯一标识一局游戏',
    title        VARCHAR(255) NOT NULL COMMENT '题目标题',
    solution     TEXT         NOT NULL COMMENT '谜底/真相',
    completed_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '完成时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_room_id (room_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='海龟汤游戏完成记录';
