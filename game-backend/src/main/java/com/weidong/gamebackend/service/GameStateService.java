package com.weidong.gamebackend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 游戏状态机（Redis 持久化，按会话维度）。
 * 状态由后端确定性驱动，不再依赖前端解析 AI 文本中的"游戏结束"标记：
 * - RUNNING: 会话进行中（键不存在即视为 RUNNING，无需显式初始化——
 *   前端每次开局生成新 memoryId，天然是干净状态）
 * - SOLVED:  玩家猜对，由 GameResultTool 落库成功后写入（工具在模型回调线程执行）
 * - REVEALED: 玩家放弃/揭晓，由 /chat 揭晓分支走结构化输出落库成功后写入
 * 状态用 Redis 而非内存: 与聊天记忆同一套持久化基础设施，多实例部署下状态一致；
 * TTL 与记忆一致（7 天），活跃会话不断刷新。
 */
@Service
public class GameStateService {

    private static final String KEY_PREFIX = "hgt:game-state:";
    private static final String QUESTION_KEY_PREFIX = "hgt:game-question:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public GameStateService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 读取会话状态，键不存在时返回 RUNNING */
    public GameState get(String memoryId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + memoryId);
        return value == null ? GameState.RUNNING : GameState.valueOf(value);
    }

    /** 会话是否已结束（SOLVED/REVEALED），已结束的会话拒绝继续对话 */
    public boolean isTerminal(String memoryId) {
        return get(memoryId) != GameState.RUNNING;
    }

    public void markSolved(String memoryId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + memoryId, GameState.SOLVED.name(), TTL);
    }

    public void markRevealed(String memoryId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + memoryId, GameState.REVEALED.name(), TTL);
    }

    /** 记录本局抽中的题库题目（开局从题库出题时写入；LLM 即兴局不写入） */
    public void setQuestion(String memoryId, Long questionId) {
        redisTemplate.opsForValue().set(QUESTION_KEY_PREFIX + memoryId, String.valueOf(questionId), TTL);
    }

    /** 本局抽中的题库题目ID，null 表示 LLM 即兴局 */
    public Long getQuestion(String memoryId) {
        String value = redisTemplate.opsForValue().get(QUESTION_KEY_PREFIX + memoryId);
        return value == null ? null : Long.valueOf(value);
    }

    public enum GameState {
        /** 游戏进行中 */
        RUNNING,
        /** 玩家猜对真相，游戏结束 */
        SOLVED,
        /** 玩家放弃，主持人揭晓真相，游戏结束 */
        REVEALED
    }
}
