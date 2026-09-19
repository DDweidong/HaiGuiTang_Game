package com.weidong.gamebackend.assistant;

/**
 * /chat 请求的用户上下文，用 ThreadLocal 从 Controller 传给 ChatModelListener。
 * 模型调用与 Controller 在同一线程同步执行，ThreadLocal 是可靠的；
 * 不用 @V 之类的参数传播——LangChain4j 会把参数暴露给 LLM 自由发挥（W2 踩坑），
 * 也不依赖框架内部是否透传 attributes，取用路径完全由代码控制。
 */
public final class ChatUsageContext {

    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private ChatUsageContext() {
    }

    public static void set(Long userId, String roomId) {
        CURRENT.set(new Context(userId, roomId));
    }

    public static Context get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record Context(Long userId, String roomId) {
    }
}
