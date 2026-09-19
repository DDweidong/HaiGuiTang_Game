package com.weidong.gamebackend.assistant;

import com.weidong.gamebackend.model.TokenUsageRecord;
import com.weidong.gamebackend.service.TokenUsageService;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 每次模型调用完成后记录 token 用量（含工具调用的额外轮次）。
 * 由 DashScope 自动配置注册到 qwenChatModel / qwenStreamingChatModel 上（声明为 bean 即生效），
 * 因此后续改造 SSE 流式时统计逻辑无需改动。
 * 落库失败只记日志——统计是旁路功能，绝不影响游戏主流程。
 */
@Component
public class TokenUsageListener implements ChatModelListener {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageListener.class);

    private final TokenUsageService tokenUsageService;

    public TokenUsageListener(TokenUsageService tokenUsageService) {
        this.tokenUsageService = tokenUsageService;
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        ChatUsageContext.Context ctx = ChatUsageContext.get();
        // 无上下文说明这次调用不来自 /chat（如启动探测），不统计
        if (ctx == null) {
            return;
        }

        TokenUsage usage = context.chatResponse().tokenUsage();
        if (usage == null || usage.totalTokenCount() == null) {
            return;
        }

        TokenUsageRecord record = new TokenUsageRecord();
        record.setUserId(ctx.userId());
        record.setRoomId(ctx.roomId());
        record.setModel(context.chatResponse().modelName());
        record.setInputTokens(usage.inputTokenCount() == null ? 0 : usage.inputTokenCount());
        record.setOutputTokens(usage.outputTokenCount() == null ? 0 : usage.outputTokenCount());
        try {
            tokenUsageService.record(record);
        } catch (Exception e) {
            log.warn("token 用量落库失败（不影响游戏流程）: roomId={}", ctx.roomId(), e);
        }
    }
}
