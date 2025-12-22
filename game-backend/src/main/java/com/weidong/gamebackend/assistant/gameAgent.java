package com.weidong.gamebackend.assistant;


import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;


@AiService(
        chatModel = "qwenChatModel",
        chatMemoryProvider ="chatMemoryProvider",
        tools = "gameResultTool"  // ← 直接注册工具
)
public interface gameAgent {

    @SystemMessage(fromResource ="haiguitang-prompt-template.txt")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);

}
