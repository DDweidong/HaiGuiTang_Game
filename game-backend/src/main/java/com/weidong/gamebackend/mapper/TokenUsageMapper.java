package com.weidong.gamebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weidong.gamebackend.model.TokenUsageRecord;
import com.weidong.gamebackend.vo.TokenUsageSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * token 用量 Mapper。
 * 单表 CRUD 走 BaseMapper；汇总统计是跨行聚合，用注解 SQL。
 */
@Mapper
public interface TokenUsageMapper extends BaseMapper<TokenUsageRecord> {

    /** 按用户汇总累计用量（无记录时返回一行全 0/空，费用在 service 层按单价换算） */
    @Select("SELECT COUNT(*) AS totalCalls, " +
            "       COALESCE(SUM(input_tokens), 0)  AS totalInputTokens, " +
            "       COALESCE(SUM(output_tokens), 0) AS totalOutputTokens, " +
            "       MIN(created_at) AS firstUsedAt, " +
            "       MAX(created_at) AS lastUsedAt " +
            "FROM token_usage WHERE user_id = #{userId}")
    TokenUsageSummaryVO summary(Long userId);
}
