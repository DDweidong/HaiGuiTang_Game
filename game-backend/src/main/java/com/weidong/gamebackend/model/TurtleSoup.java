package com.weidong.gamebackend.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 海龟汤完成记录实体，对应表 completed_games。
 * 表结构由 Flyway 迁移脚本 db/migration/V1__init.sql 管理。
 */
@Data
@NoArgsConstructor
@TableName("completed_games")
public class TurtleSoup {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private String userId;

    /** 游戏会话ID（即 memoryId），一局游戏唯一 */
    private String roomId;

    /** 题目标题 */
    private String title;

    /** 谜底/真相 */
    private String solution;

    /** 完成时间 */
    private LocalDateTime completedAt;
}
