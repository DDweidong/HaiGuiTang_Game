package com.weidong.gamebackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 完成记录视图对象。
 * 只暴露前端需要的字段，不暴露实体内部的 userId / roomId。
 */
@Data
public class TurtleSoupVO {

    private Long id;

    /** 题目标题 */
    private String title;

    /** 谜底/真相 */
    private String solution;

    /** 完成时间 */
    private LocalDateTime completedAt;
}
