package com.weidong.gamebackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 保存完成记录请求体。id / completedAt 由服务端生成，不接受客户端传入。
 */
@Data
@NoArgsConstructor
public class TurtleSoupCreateRequest {

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "会话ID不能为空")
    private String roomId;

    @NotBlank(message = "题目标题不能为空")
    private String title;

    @NotBlank(message = "谜底内容不能为空")
    private String solution;
}
