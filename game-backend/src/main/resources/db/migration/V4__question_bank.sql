-- 海龟汤题库表。
-- 开局从题库抽题（该用户没玩过的优先），题库的汤面/汤底由用户自己整理，
-- 解决 LLM 即兴出题同质化问题（RAG 被评估为不适用，见规划文档 3.2）。
CREATE TABLE IF NOT EXISTS soup_questions (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title      VARCHAR(255) NOT NULL COMMENT '题目标题',
    tang_mian  TEXT         NOT NULL COMMENT '汤面（情境描述，开局展示给玩家）',
    tang_di    TEXT         NOT NULL COMMENT '汤底（完整真相，仅主持人可见）',
    difficulty VARCHAR(10)  NOT NULL DEFAULT '中等' COMMENT '难度: 简单/中等/困难',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (id),
    KEY idx_difficulty (difficulty)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='海龟汤题库';

-- 完成记录关联题库题目: 用于"该用户没玩过"的抽取过滤；
-- LLM 即兴出题的记录该列为 NULL（老数据迁移后同为 NULL）
ALTER TABLE completed_games
    ADD COLUMN question_id BIGINT NULL COMMENT '题库题目ID（NULL=LLM 即兴出题）' AFTER room_id;
