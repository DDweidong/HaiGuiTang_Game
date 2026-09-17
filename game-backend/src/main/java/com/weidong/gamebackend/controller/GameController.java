package com.weidong.gamebackend.controller;

import com.weidong.gamebackend.assistant.GameAgent;
import com.weidong.gamebackend.common.Result;
import com.weidong.gamebackend.dto.ChatRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "游戏对话", description = "与 AI 主持人的对话接口")
public class GameController {

    @Autowired
    private GameAgent gameAgent;

    /**
     * 与 AI 主持人对话。
     * 注意: 当前为同步阻塞调用，LLM 生成耗时较长（W3 规划改造为 SSE 流式）。
     */
    @PutMapping("/chat")
    @Operation(summary = "与 AI 主持人对话", description = "返回 AI 的完整回复文本；当回复首行为\"游戏结束\"时表示本局结束")
    public Result<String> chat(@Valid @RequestBody ChatRequest request) {
        String reply = gameAgent.chat(request.getMemoryId(), request.getMessage());
        return Result.success(reply);
    }
}
