package com.weidong.gamebackend.service;

import com.weidong.gamebackend.common.PageResult;
import com.weidong.gamebackend.model.TokenUsageRecord;
import com.weidong.gamebackend.vo.TokenUsageSummaryVO;
import com.weidong.gamebackend.vo.TokenUsageVO;

/**
 * LLM token 用量统计。
 */
public interface TokenUsageService {

    /** 落一条用量记录（由 ChatModelListener 在每次模型调用后触发） */
    void record(TokenUsageRecord record);

    /** 当前用户的累计用量 + 估算费用 */
    TokenUsageSummaryVO summary(Long userId);

    /** 当前用户的调用明细，按时间倒序分页 */
    PageResult<TokenUsageVO> pageByUser(Long userId, long pageNum, long pageSize);
}
