package com.weidong.gamebackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weidong.gamebackend.assistant.GameAgent;
import com.weidong.gamebackend.assistant.GameReveal;
import com.weidong.gamebackend.assistant.GameRevealAgent;
import com.weidong.gamebackend.assistant.SyncUsageContext;
import com.weidong.gamebackend.common.ErrorCode;
import com.weidong.gamebackend.common.exception.BusinessException;
import com.weidong.gamebackend.dto.ChatRequest;
import com.weidong.gamebackend.model.TokenUsageRecord;
import com.weidong.gamebackend.model.TurtleSoup;
import com.weidong.gamebackend.security.LoginUser;
import com.weidong.gamebackend.security.SecurityUtil;
import com.weidong.gamebackend.service.GameStateService;
import com.weidong.gamebackend.service.RateLimitService;
import com.weidong.gamebackend.service.TokenUsageService;
import com.weidong.gamebackend.service.TurtleSoupService;
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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@RestController
@Tag(name = "游戏对话", description = "与 AI 主持人的对话接口")
public class GameController {

    /** LLM 单次对话最长耗时（含多轮工具调用），超时由 SseEmitter 自动断开连接 */
    private static final long STREAM_TIMEOUT_MS = 10 * 60 * 1000L;

    /** 揭晓类请求触发词（与前端 30 条上限时发送的文案保持一致），由后端识别，前端不可绕过 */
    private static final Pattern REVEAL_PATTERN = Pattern.compile("公布答案|揭晓答案|结束游戏|猜不出来|放弃|不玩了");

    @Autowired
    private GameAgent gameAgent;

    @Autowired
    private GameRevealAgent gameRevealAgent;

    @Autowired
    private GameStateService gameStateService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private TokenUsageService tokenUsageService;

    @Autowired
    private TurtleSoupService turtleSoupService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 与 AI 主持人对话（SSE 流式）。
     * 事件协议: 每条消息为一行 "data: <JSON>"，JSON 的 type 字段取值：
     * - token: 增量文本片段（text 字段），前端把所有片段拼接即为完整回复
     * - done: 生成结束，state 字段表示会话终态（RUNNING/SOLVED/REVEALED），
     *         前端据此判定游戏结束——状态由后端状态机驱动，不解析 AI 文本
     * - error: 出错（message 字段）
     * 揭晓类消息走结构化输出分支（非流式，回复一次性作为 token 事件发送）。
     */
    @PutMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "与 AI 主持人对话（SSE 流式）", description = "以 SSE 事件流返回 AI 回复，done 事件携带会话状态")
    public SseEmitter chat(@Valid @RequestBody ChatRequest request) {
        LoginUser loginUser = SecurityUtil.currentUser();

        // memoryId 约定为 "userId/timestamp"，校验前缀与登录用户一致，
        // 防止伪造他人会话、读写他人对话记忆
        String memoryId = request.getMemoryId();
        String expectedPrefix = loginUser.id() + "/";
        if (memoryId == null || !memoryId.startsWith(expectedPrefix)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "会话不属于当前用户");
        }

        // 已结束的会话拒绝继续对话（状态机在 Redis，见 GameStateService）
        if (gameStateService.isTerminal(memoryId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "本局游戏已结束，请返回首页开始新的一局");
        }

        // 限流: 按用户计数，防止脚本刷量烧 token（超限抛 42900）
        rateLimitService.check(loginUser.id());

        if (REVEAL_PATTERN.matcher(request.getMessage()).find()) {
            return reveal(loginUser, memoryId, request.getMessage());
        }
        return streamReply(loginUser, memoryId, request.getMessage());
    }

    /**
     * 常规提问: 流式生成。
     * 回调运行在 DashScope SDK 的异步线程上，ThreadLocal 的安全上下文不传播，
     * userId/memoryId 由闭包捕获（GameResultTool 有同类兜底处理）。
     */
    private SseEmitter streamReply(LoginUser loginUser, String memoryId, String message) {
        Long userId = loginUser.id();
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);

        TokenStream stream = gameAgent.chatStream(memoryId, message);
        stream.onPartialResponse(token -> sendEvent(emitter, Map.of("type", "token", "text", token)))
                .onCompleteResponse(response -> {
                    recordUsage(userId, memoryId, response);
                    // 猜对时 GameResultTool 已在回调线程落库并置状态 SOLVED，这里直接读终态
                    GameStateService.GameState state = gameStateService.get(memoryId);
                    sendEvent(emitter, Map.of("type", "done", "state", state.name()));
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
     * 揭晓分支: 结构化输出（非流式）。
     * 由 GameRevealAgent 以类型化 JSON 返回 {title, solution, reply}，业务代码自行落库——
     * 不依赖 LLM 自主调用保存工具，根治"只恭喜不落库"；
     * 同步调用与请求同线程，userId 直接从 SecurityContext 取。
     */
    private SseEmitter reveal(LoginUser loginUser, String memoryId, String message) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);

        try {
            // 设置用量归属上下文，SyncUsageListener 在每次模型调用完成后按它记录
            SyncUsageContext.set(loginUser.id(), memoryId);
            GameReveal reveal = gameRevealAgent.reveal(memoryId, message);

            TurtleSoup record = new TurtleSoup();
            record.setUserId(String.valueOf(loginUser.id()));
            record.setRoomId(memoryId);
            record.setTitle(reveal.title());
            record.setSolution(reveal.solution());
            record.setCompletedAt(LocalDateTime.now());
            turtleSoupService.saveTurtleSoup(record);
            gameStateService.markRevealed(memoryId);

            sendEvent(emitter, Map.of("type", "token", "text", reveal.reply()));
            sendEvent(emitter, Map.of("type", "done", "state", GameStateService.GameState.REVEALED.name()));
            emitter.complete();
        } catch (Exception e) {
            // 揭晓失败不置终态，玩家可重试；记录已保存但状态置位失败的情况以日志定位
            log.error("揭晓失败, memoryId={}", memoryId, e);
            sendEvent(emitter, Map.of("type", "error", "message", "揭晓失败，请重试"));
            emitter.complete();
        } finally {
            SyncUsageContext.clear();
        }
        return emitter;
    }

    /**
     * 记录本次流式对话的 token 用量（每次 /chat 一条聚合记录）。
     * onCompleteResponse 收到的 usage 已包含全部工具调用轮次（LangChain4j 内部累加）。
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
