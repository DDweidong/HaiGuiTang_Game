package com.weidong.gamebackend.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * 海龟汤游戏主持人 Agent。
 * LangChain4j 会在启动时为该接口生成动态代理：
 * - chatModel: 使用 DashScope starter 自动装配的 qwen 对话模型 Bean；
 * - chatMemoryProvider: 按 memoryId 隔离的多轮对话记忆（见 GameAgentConfig）；
 * - tools: 注册给 LLM 的工具，LLM 可在生成过程中自主决定调用（如猜对时保存记录）。
 * 游戏的全部规则在系统提示词 haiguitang-prompt-template.txt 中，业务代码不含游戏逻辑。
 */
@AiService(
        chatModel = "qwenChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = "gameResultTool"
)
public interface GameAgent {

    @SystemMessage(fromResource = "haiguitang-prompt-template.txt")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);
}
