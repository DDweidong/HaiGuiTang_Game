package com.weidong.gamebackend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户 token 用量汇总（累计值 + 估算费用）。
 * 费用按官方单价估算（qwen-max: 输入 2.4 元/百万 token、输出 9.6 元/百万 token，2026-09 阿里云百炼定价），
 * 单价在 application.yml 配置，模型调价时改配置即可，不写死在代码里。
 */
@Data
public class TokenUsageSummaryVO {

    /** 累计模型调用次数 */
    private Long totalCalls;

    /** 累计输入 token */
    private Long totalInputTokens;

    /** 累计输出 token */
    private Long totalOutputTokens;

    /** 估算费用（元） */
    private BigDecimal estimatedCost;

    /** 首次调用时间（从未调用过为 null） */
    private LocalDateTime firstUsedAt;

    /** 最近一次调用时间 */
    private LocalDateTime lastUsedAt;
}
