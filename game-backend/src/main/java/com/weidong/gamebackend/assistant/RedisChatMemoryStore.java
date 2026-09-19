package com.weidong.gamebackend.assistant;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 聊天记忆的 Redis 持久化实现。
 * MessageWindowChatMemory 只负责"最多 60 条"的窗口裁剪，数据落到哪由 ChatMemoryStore 决定；
 * 默认 InMemory 实现服务重启即丢，且无法在多实例间共享，这里换成 Redis。
 * 消息是 AI/用户/工具结果等多态类型，用 LangChain4j 官方 codec 序列化为 JSON（自带类型标注），
 * 避免手写 Jackson 多态配置。StringRedisTemplate 存字符串，配合 codec 不需要 Redis 序列化器定制。
 */
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "hgt:chat-memory:";
    /** 会话记忆保留 7 天（与 refresh token 有效期一致），到期自动清理，避免 Redis 无限堆积 */
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public RedisChatMemoryStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redisTemplate.opsForValue().get(key(memoryId));
        return json == null ? List.of() : ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        // 每次写入刷新 TTL: 活跃会话持续保留，超一周不聊自动过期
        redisTemplate.opsForValue().set(key(memoryId), ChatMessageSerializer.messagesToJson(messages), TTL);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(key(memoryId));
    }

    private String key(Object memoryId) {
        return KEY_PREFIX + memoryId;
    }
}
