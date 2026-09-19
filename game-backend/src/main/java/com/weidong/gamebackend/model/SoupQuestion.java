package com.weidong.gamebackend.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 海龟汤题库实体，对应表 soup_questions。
 * 题库由用户自己整理（解决 LLM 即兴出题同质化问题），
 * 汤底（真相）只注入给主持人会话，玩家不可见。
 */
@Data
@NoArgsConstructor
@TableName("soup_questions")
public class SoupQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目标题 */
    private String title;

    /** 汤面（情境描述，开局展示给玩家） */
    private String tangMian;

    /** 汤底（完整真相，仅主持人可见） */
    private String tangDi;

    /** 难度: 简单/中等/困难 */
    private String difficulty;

    /** 入库时间 */
    private LocalDateTime createdAt;
}
