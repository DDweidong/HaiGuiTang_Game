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
 * 同步模型调用（揭晓分支）的 token 用量记录。
 * 声明为 bean 即被 DashScope 自动配置注册到 qwenChatModel / qwenStreamingChatModel；
 * 流式路径的用量由 /chat 的 onCompleteResponse 聚合落库，不设置 {@link SyncUsageContext}，
 * 本 listener 对其直接跳过，两条路径互不重复。
 * 落库失败只记日志——统计是旁路功能，绝不影响游戏主流程。
 */
@Component
public class SyncUsageListener implements ChatModelListener {

    private static final Logger log = LoggerFactory.getLogger(SyncUsageListener.class);

    private final TokenUsageService tokenUsageService;

    public SyncUsageListener(TokenUsageService tokenUsageService) {
        this.tokenUsageService = tokenUsageService;
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        SyncUsageContext.Context ctx = SyncUsageContext.get();
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
