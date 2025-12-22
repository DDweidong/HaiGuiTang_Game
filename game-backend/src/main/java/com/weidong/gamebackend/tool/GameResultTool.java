package com.weidong.gamebackend.tool;

import com.weidong.gamebackend.model.TurtleSoup;
import com.weidong.gamebackend.service.GameResultService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class GameResultTool {

    @Autowired
    private GameResultService gameResultService;

    /**
     * 当玩家猜对海龟汤真相时，调用此工具保存游戏记录。
     *
     * @param memoryId  会话ID，格式为 "userId/timestamp"
     * @param title     题目标题
     * @param solution  真相内容
     */
    @Tool("当用户猜对海龟汤谜题时，调用此方法保存游戏完成记录")
    public String saveGameResult(@ToolMemoryId String memoryId, String title, String solution) {
        try {
            // 拆解 userId
            if (!memoryId.contains("/")) {
                return "memoryId 格式错误，无法保存结果";
            }
            String userId = memoryId.split("/", 2)[0];

            // 构造实体
            TurtleSoup record = new TurtleSoup();
            record.setUserId(userId);
            record.setRoomId(memoryId); // 整个 memoryId 作为 roomId（唯一）
            record.setTitle(title);
            record.setSolution(solution);
            record.setCompletedAt(LocalDateTime.now());

            // 保存到数据库
            gameResultService.saveTurtleSoup(record);

            return "恭喜！游戏记录已成功保存。";
        } catch (Exception e) {
            e.printStackTrace();
            return "保存失败，请稍后再试。";
        }
    }

}