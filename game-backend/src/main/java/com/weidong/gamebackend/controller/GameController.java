package com.weidong.gamebackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weidong.gamebackend.assistant.GameAgent;
import com.weidong.gamebackend.common.ErrorCode;
import com.weidong.gamebackend.common.exception.BusinessException;
import com.weidong.gamebackend.dto.ChatRequest;
import com.weidong.gamebackend.model.TokenUsageRecord;
import com.weidong.gamebackend.security.LoginUser;
import com.weidong.gamebackend.security.SecurityUtil;
import com.weidong.gamebackend.service.RateLimitService;
import com.weidong.gamebackend.service.TokenUsageService;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.TokenStream;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@Tag(name = "游戏对话", description = "与 AI 主持人的对话接口")
public class GameController {

    /** LLM 单次对话最长耗时（含多轮工具调用），超时由 SseEmitter 自动断开连接 */
    private static final long STREAM_TIMEOUT_MS = 10 * 60 * 1000L;

    @Autowired
    private GameAgent gameAgent;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private TokenUsageService tokenUsageService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 与 AI 主持人对话（SSE 流式）。
     * 事件协议: 每条消息为一行 "data: <JSON>"，JSON 的 type 字段取值：
     * - token: 增量文本片段（text 字段），前端把所有片段拼接即为完整回复
     * - done: 生成结束
     * - error: 出错（message 字段）
     * "游戏结束"判定逻辑不变：前端拼接完整回复后按首行标记判断。
     */
    @PutMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "与 AI 主持人对话（SSE 流式）", description = "以 SSE 事件流返回 AI 回复，事件类型: token/done/error")
    public SseEmitter chat(@Valid @RequestBody ChatRequest request) {
        LoginUser loginUser = SecurityUtil.currentUser();

        // memoryId 约定为 "userId/timestamp"，校验前缀与登录用户一致，
        // 防止伪造他人会话、读写他人对话记忆
        String memoryId = request.getMemoryId();
        String expectedPrefix = loginUser.id() + "/";
        if (memoryId == null || !memoryId.startsWith(expectedPrefix)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "会话不属于当前用户");
        }

        // 限流: 按用户计数，防止脚本刷量烧 token（超限抛 42900）
        rateLimitService.check(loginUser.id());

        Long userId = loginUser.id();
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);

        // 回调运行在 DashScope SDK 的异步线程上，ThreadLocal 的安全上下文不传播，
        // userId/memoryId 由闭包捕获（GameResultTool 有同类兜底处理）
        TokenStream stream = gameAgent.chatStream(memoryId, request.getMessage());
        stream.onPartialResponse(token -> sendEvent(emitter, Map.of("type", "token", "text", token)))
                .onCompleteResponse(response -> {
                    recordUsage(userId, memoryId, response);
                    sendEvent(emitter, Map.of("type", "done"));
                    emitter.complete();
                })
                .onError(throwable -> {
                    log.error("AI 流式生成失败, memoryId={}", memoryId, throwable);
                    sendEvent(emitter, Map.of("type", "error", "message", "AI 生成失败，请重试"));
                    emitter.complete();
                });
        try {
            stream.start();
        } catch (Exception e) {
            log.error("启动 AI 流式生成失败, memoryId={}", memoryId, e);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    /**
     * 记录本次对话的 token 用量（每次 /chat 一条聚合记录）。
     * onCompleteResponse 收到的 usage 已包含全部工具调用轮次（LangChain4j 内部累加），
     * 因此这里直接落一条总量，不需要再依赖每次模型调用的 listener。
     * 统计是旁路功能: 落库失败只记日志，绝不影响游戏主流程。
     */
    private void recordUsage(Long userId, String memoryId, ChatResponse response) {
        TokenUsage usage = response.metadata().tokenUsage();
        if (usage == null || usage.totalTokenCount() == null) {
            return;
        }
        TokenUsageRecord record = new TokenUsageRecord();
        record.setUserId(userId);
        record.setRoomId(memoryId);
        record.setModel(response.modelName());
        record.setInputTokens(usage.inputTokenCount() == null ? 0 : usage.inputTokenCount());
        record.setOutputTokens(usage.outputTokenCount() == null ? 0 : usage.outputTokenCount());
        try {
            tokenUsageService.record(record);
        } catch (Exception e) {
            log.warn("token 用量落库失败（不影响游戏流程）: roomId={}", memoryId, e);
        }
    }

    /** 发送一条 SSE 事件；客户端已断开时静默结束连接 */
    private void sendEvent(SseEmitter emitter, Object payload) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(payload)));
        } catch (IOException e) {
            log.debug("SSE 客户端已断开, 停止发送");
            emitter.complete();
        } catch (Exception e) {
            log.warn("SSE 事件发送失败", e);
        }
    }
}
