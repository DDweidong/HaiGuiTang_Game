package com.weidong.gamebackend.configuration;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 记忆配置。
 * 每个 memoryId 维护独立的消息窗口，只保留最近 60 条消息：
 * 一方面控制发给 LLM 的上下文长度、节省 token 成本，另一方面避免长对话超出模型上下文上限。
 * 当前实现存于内存，服务重启即丢失（Redis 持久化在 W3 规划中）。
 */
@Configuration
public class GameAgentConfig {

    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(60)
                .build();
    }
}
