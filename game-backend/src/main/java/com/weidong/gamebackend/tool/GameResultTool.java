package com.weidong.gamebackend.tool;

import com.weidong.gamebackend.model.TurtleSoup;
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
     *
     * @param memoryId 会话ID，格式为 "userId/timestamp"
     * @param title    题目标题
     * @param solution 真相内容
     */
    @Tool("当用户猜对海龟汤谜题时，调用此方法保存游戏完成记录")
    public String saveGameResult(@ToolMemoryId String memoryId, String title, String solution) {
        try {
            // memoryId 约定为 "userId/timestamp"，取前缀得到用户ID
            if (!memoryId.contains("/")) {
                return "memoryId 格式错误，无法保存结果";
            }
            String userId = memoryId.split("/", 2)[0];

            TurtleSoup record = new TurtleSoup();
            record.setUserId(userId);
            record.setRoomId(memoryId);
            record.setTitle(title);
            record.setSolution(solution);
            record.setCompletedAt(LocalDateTime.now());

            turtleSoupService.saveTurtleSoup(record);

            return "恭喜！游戏记录已成功保存。";
        } catch (Exception e) {
            log.error("保存游戏记录失败, memoryId={}", memoryId, e);
            return "保存失败，请稍后再试。";
        }
    }
}
