package com.weidong.gamebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidong.gamebackend.common.PageResult;
import com.weidong.gamebackend.mapper.TokenUsageMapper;
import com.weidong.gamebackend.model.TokenUsageRecord;
import com.weidong.gamebackend.service.TokenUsageService;
import com.weidong.gamebackend.vo.TokenUsageSummaryVO;
import com.weidong.gamebackend.vo.TokenUsageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TokenUsageServiceImpl extends ServiceImpl<TokenUsageMapper, TokenUsageRecord> implements TokenUsageService {

    private final BigDecimal inputYuanPer1m;
    private final BigDecimal outputYuanPer1m;

    public TokenUsageServiceImpl(@Value("${hgt.pricing.input-yuan-per-1m}") BigDecimal inputYuanPer1m,
                                 @Value("${hgt.pricing.output-yuan-per-1m}") BigDecimal outputYuanPer1m) {
        this.inputYuanPer1m = inputYuanPer1m;
        this.outputYuanPer1m = outputYuanPer1m;
    }

    @Override
    public void record(TokenUsageRecord record) {
        save(record);
    }

    @Override
    public TokenUsageSummaryVO summary(Long userId) {
        TokenUsageSummaryVO vo = baseMapper.summary(userId);
        BigDecimal cost = new BigDecimal(vo.getTotalInputTokens())
                .multiply(inputYuanPer1m)
                .add(new BigDecimal(vo.getTotalOutputTokens()).multiply(outputYuanPer1m))
                .divide(BigDecimal.valueOf(1_000_000), 4, RoundingMode.HALF_UP);
        vo.setEstimatedCost(cost);
        return vo;
    }

    @Override
    public PageResult<TokenUsageVO> pageByUser(Long userId, long pageNum, long pageSize) {
        Page<TokenUsageRecord> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<TokenUsageRecord>()
                        .eq(TokenUsageRecord::getUserId, userId)
                        .orderByDesc(TokenUsageRecord::getCreatedAt));
        return PageResult.of(
                page.getRecords().stream().map(this::toVo).toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize());
    }

    /** 实体转 VO，避免把 userId/roomId 暴露给前端；token 数实体是 Integer、VO 是 Long，需手动赋值 */
    private TokenUsageVO toVo(TokenUsageRecord record) {
        TokenUsageVO vo = new TokenUsageVO();
        BeanUtils.copyProperties(record, vo, "inputTokens", "outputTokens");
        vo.setInputTokens(record.getInputTokens() == null ? null : record.getInputTokens().longValue());
        vo.setOutputTokens(record.getOutputTokens() == null ? null : record.getOutputTokens().longValue());
        return vo;
    }
}
