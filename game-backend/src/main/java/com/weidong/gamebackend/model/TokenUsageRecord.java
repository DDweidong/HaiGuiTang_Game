package com.weidong.gamebackend.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * LLM token 用量记录实体，对应表 token_usage。
 * 表结构由 Flyway 迁移脚本 db/migration/V3__token_usage.sql 管理。
 */
@Data
@NoArgsConstructor
@TableName("token_usage")
public class TokenUsageRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 游戏会话ID（即 memoryId），本次调用所属的对局 */
    private String roomId;

    /** 模型名（如 qwen-max） */
    private String model;

    /** 本次调用输入 token 数 */
    private Integer inputTokens;

    /** 本次调用输出 token 数 */
    private Integer outputTokens;

    /** 调用时间 */
    private LocalDateTime createdAt;
}
