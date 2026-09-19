package com.weidong.gamebackend.tool;

import com.weidong.gamebackend.model.TurtleSoup;
import com.weidong.gamebackend.security.SecurityUtil;
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

    /**
     * 当玩家猜对海龟汤真相时，调用此工具保存游戏记录。
     * 用户身份不经过 LLM（LLM 不可见、不可伪造）：工具与 /chat 在同一请求线程内
     * 同步执行，直接从 Spring Security 上下文取登录用户。
     * （曾尝试 LangChain4j @V 参数传播，1.20.0-beta30 中未生效——
     * userId 参数被暴露给 LLM 并被其自由发挥填成"玩家123"，见规划文档 §7 踩坑记录。）
     *
     * @param memoryId 会话ID，格式为 "userId/timestamp"
     * @param title    题目标题
     * @param solution 真相内容
     */
    @Tool("当游戏结束时（玩家猜对真相，或玩家要求公布答案/结束游戏），调用此方法保存游戏完成记录")
    public String saveGameResult(@ToolMemoryId String memoryId, String title, String solution) {
        // 入口日志: 用于区分"LLM 未调用工具"与"调用后执行失败"两类问题
        log.info("收到保存游戏记录请求, memoryId={}, title={}", memoryId, title);
        try {
            TurtleSoup record = new TurtleSoup();
            record.setUserId(String.valueOf(SecurityUtil.currentUser().id()));
            record.setRoomId(memoryId);
            record.setTitle(title);
            record.setSolution(solution);
            record.setCompletedAt(LocalDateTime.now());

            turtleSoupService.saveTurtleSoup(record);

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
}
