package com.weidong.gamebackend.service;

import com.weidong.gamebackend.common.ErrorCode;
import com.weidong.gamebackend.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * /chat 接口限流（固定窗口计数，按用户维度）。
 * 需求: LLM 按 token 计费，/chat 是唯一直接烧模型费用的接口，小范围公开后需防脚本刷量。
 * 为什么固定窗口 + Lua: 单后端低流量场景下固定窗口足够简单精确；INCR 与首次设置过期
 * 是两条命令，拆开执行存在竞态（窗口重置被打断则计数永不归零），用 Lua 脚本保证原子性。
 * 阈值可配置（hgt.rate-limit.chat.*），上线后按真实流量调整。
 */
@Service
public class RateLimitService {

    private static final String KEY_PREFIX = "hgt:rate:chat:";

    private static final DefaultRedisScript<Long> INCR_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]) " +
                    "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
                    "return current",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final long maxRequests;
    private final long windowSeconds;

    public RateLimitService(StringRedisTemplate redisTemplate,
                            @Value("${hgt.rate-limit.chat.max-requests}") long maxRequests,
                            @Value("${hgt.rate-limit.chat.window-seconds}") long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    /** 计数并校验是否超限，超限抛 BusinessException(42900)，由全局异常处理输出统一响应 */
    public void check(Long userId) {
        Long count = redisTemplate.execute(INCR_SCRIPT, List.of(KEY_PREFIX + userId), String.valueOf(windowSeconds));
        if (count != null && count > maxRequests) {
            throw new BusinessException(ErrorCode.RATE_LIMITED);
        }
    }
}
