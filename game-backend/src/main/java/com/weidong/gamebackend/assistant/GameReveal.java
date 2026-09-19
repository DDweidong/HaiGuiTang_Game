package com.weidong.gamebackend.assistant;

/**
 * 揭晓结构化输出。
 * GameRevealAgent.reveal 的类型化返回，LangChain4j 会要求模型按该结构输出 JSON
 * （模型支持 response_format 时走 json 模式，否则自动回退为工具调用式结构化输出），
 * 由框架反序列化为本记录，业务代码拿到的是确定字段而非自由文本。
 *
 * @param title    汤面标题
 * @param solution 完整真相
 * @param reply    展示给玩家的揭晓文案
 */
public record GameReveal(String title, String solution, String reply) {
}
