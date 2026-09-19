package com.weidong.gamebackend.assistant;

/**
 * 同步模型调用的用量归属上下文（ThreadLocal）。
 * 仅揭晓分支使用: GameRevealAgent 是同步调用，与 /chat 同一请求线程，
 * 无法像流式路径那样在 onCompleteResponse 里拿到聚合 TokenUsage，
 * 因此由 SyncUsageListener 在每次模型调用完成后按此上下文记录归属。
 * 流式路径不设置本上下文，listener 跳过，避免与流式聚合记录重复计数。
 */
public final class SyncUsageContext {

    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private SyncUsageContext() {
    }

    public static void set(Long userId, String roomId) {
        HOLDER.set(new Context(userId, roomId));
    }

    public static Context get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public record Context(Long userId, String roomId) {
    }
}
