package com.weidong.gamebackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单次模型调用的用量视图对象。
 * 只暴露前端需要的字段，不暴露 userId / roomId。
 */
@Data
public class TokenUsageVO {

    private Long id;

    /** 模型名（如 qwen-max） */
    private String model;

    /** 输入 token 数 */
    private Long inputTokens;

    /** 输出 token 数 */
    private Long outputTokens;

    /** 调用时间 */
    private LocalDateTime createdAt;
}
