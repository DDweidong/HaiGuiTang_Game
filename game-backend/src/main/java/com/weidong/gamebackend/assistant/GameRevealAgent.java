package com.weidong.gamebackend.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * 揭晓 Agent（结构化输出）。
 * 与 GameAgent 独立: 揭晓是游戏最后一轮，不需要流式体验，且结构化输出
 * 在 1.20.0-beta30 只支持非流式模型，故使用同步的 qwenChatModel。
 * 与 GameAgent 共享 chatMemoryProvider（同一 Redis 存储、同一 memoryId 键），
 * 因此能看到完整对话历史，从而确定汤面标题与真相。
 * 返回类型化为 {@link GameReveal}，落库由业务代码完成——
 * 根治"LLM 只恭喜不调用保存工具"：揭晓场景不再依赖 LLM 自主调用工具。
 */
@AiService(
        chatModel = "qwenChatModel",
        chatMemoryProvider = "chatMemoryProvider"
)
public interface GameRevealAgent {

    @SystemMessage(fromResource = "haiguitang-reveal-template.txt")
    GameReveal reveal(@MemoryId String memoryId, @UserMessage String userMessage);
}
