-- LLM token 用量表（W3 成本统计: 每次模型调用一行，含工具调用的额外轮次）
-- 按调用计费: 同一次 /chat 若触发工具（如猜对保存记录），会有两行，求和即该次请求的真实消耗
CREATE TABLE IF NOT EXISTS token_usage (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT       NOT NULL COMMENT '用户ID(users.id)',
    room_id       VARCHAR(128) NOT NULL COMMENT '游戏会话ID(memoryId)，本次调用所属的对局',
    model         VARCHAR(64)  NOT NULL COMMENT '模型名(如 qwen-max)',
    input_tokens  INT          NOT NULL COMMENT '本次调用输入 token 数',
    output_tokens INT          NOT NULL COMMENT '本次调用输出 token 数',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
    PRIMARY KEY (id),
    KEY idx_user_created (user_id, created_at),
    KEY idx_room_id (room_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='LLM token 用量记录';
