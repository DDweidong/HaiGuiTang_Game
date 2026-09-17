package com.weidong.gamebackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * /chat 请求体。
 */
@Data
@NoArgsConstructor
public class ChatRequest {

    /** 会话ID，格式约定为 "userId/时间戳"，由前端生成 */
    @NotBlank(message = "会话ID不能为空")
    private String memoryId;

    /** 用户消息内容 */
    @NotBlank(message = "消息内容不能为空")
    private String message;
}
