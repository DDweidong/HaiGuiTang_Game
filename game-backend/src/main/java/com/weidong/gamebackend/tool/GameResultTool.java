package com.weidong.gamebackend.tool;

import com.weidong.gamebackend.model.TurtleSoup;
import com.weidong.gamebackend.security.SecurityUtil;
import com.weidong.gamebackend.service.GameStateService;
import com.weidong.gamebackend.service.TurtleSoupService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 游戏结果保存工具。
 * 注册到 GameAgent 上，由 LLM 在判定"玩家猜对"时自主调用（Function Calling），
 * 业务代码不感知游戏何时结束。
 */
@Slf4j
@Component
public class GameResultTool {

    @Autowired
    private TurtleSoupService turtleSoupService;

    @Autowired
    private GameStateService gameStateService;

    /**
     * 当玩家猜对海龟汤真相时，调用此工具保存游戏记录。
     * 用户身份不经过 LLM（LLM 不可见、不可伪造）。
     * 同步调用时代码与 /chat 在同一请求线程执行，可直接从 SecurityContext 取登录用户；
     * SSE 流式下工具在模型 SDK 回调线程执行、ThreadLocal 安全上下文不传播，
     * 此时回退为解析 memoryId 前缀——memoryId 已在 /chat 入口校验过归属
     * （"userId/timestamp" 格式），且该参数由 LangChain4j 注入、不经过 LLM。
     * （曾尝试 LangChain4j @V 参数传播，1.20.0-beta30 中未生效——
     * userId 参数被暴露给 LLM 并被其自由发挥填成"玩家123"，见规划文档 §7 踩坑记录。）
     *
     * @param memoryId 会话ID，格式为 "userId/timestamp"
     * @param title    题目标题
     * @param solution 真相内容
     */
    @Tool("当玩家猜对真相时，调用此方法保存游戏完成记录（玩家要求公布答案的场景由系统处理，无需调用本工具）")
    public String saveGameResult(@ToolMemoryId String memoryId, String title, String solution) {
        // 入口日志: 用于区分"LLM 未调用工具"与"调用后执行失败"两类问题
        log.info("收到保存游戏记录请求, memoryId={}, title={}", memoryId, title);
        try {
            TurtleSoup record = new TurtleSoup();
            record.setUserId(resolveUserId(memoryId));
            record.setRoomId(memoryId);
            record.setTitle(title);
            record.setSolution(solution);
            record.setCompletedAt(LocalDateTime.now());

            turtleSoupService.saveTurtleSoup(record);
            // 落库成功即置状态机终态 SOLVED，/chat 流结束后读取它放入 done 事件，
            // 前端以此结束游戏（状态驱动，不依赖 AI 文本里的"游戏结束"标记）
            gameStateService.markSolved(memoryId);

            log.info("游戏记录保存成功, memoryId={}", memoryId);
            // 注意: 该返回值会作为工具执行结果进入 LLM 上下文，参与下一轮生成，
            // 因此必须引导模型按系统提示词的结束格式回复玩家，而不是让它"确认保存成功"
            return "记录已保存。现在请严格按照系统提示词中的【最高铁律 · 游戏结束协议】回复玩家："
                    + "第一行是“游戏结束”，然后说明恭喜（若猜对）或遗憾（若未猜对），并给出完整真相。";
        } catch (Exception e) {
            log.error("保存游戏记录失败, memoryId={}", memoryId, e);
            return "保存失败，请稍后再试。";
        }
    }

    private String resolveUserId(String memoryId) {
        try {
            return String.valueOf(SecurityUtil.currentUser().id());
        } catch (Exception e) {
            return memoryId.substring(0, memoryId.indexOf('/'));
        }
    }
}
