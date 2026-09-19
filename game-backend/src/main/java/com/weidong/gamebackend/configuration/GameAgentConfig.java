package com.weidong.gamebackend.configuration;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 记忆配置。
 * 每个 memoryId 维护独立的消息窗口，只保留最近 80 条消息：
 * 一方面控制发给 LLM 的上下文长度、节省 token 成本，另一方面避免长对话超出模型上下文上限。
 * 80 条的量级依据: 前端 30 条提问强制揭晓（约 60 条问答）+
 * 开局注入的题目答案键（1 条系统消息）+ 工具调用轮次的返回值，
 * 保证答案键在整个对局内不被窗口淘汰（主持人一旦忘记真相，判断与揭晓都会出错）。
 * 数据持久化到 Redis（{@link com.weidong.gamebackend.assistant.RedisChatMemoryStore}），
 * 服务重启/多实例部署后玩家上下文不丢。
 */
@Configuration
public class GameAgentConfig {

    @Bean
    ChatMemoryProvider chatMemoryProvider(ChatMemoryStore chatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(80)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }
}
