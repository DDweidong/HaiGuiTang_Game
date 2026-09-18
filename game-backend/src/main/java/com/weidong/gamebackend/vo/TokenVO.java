package com.weidong.gamebackend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录/注册/刷新成功后的令牌响应。
 * accessToken 用于接口鉴权（30 分钟），refreshToken 用于换取新 accessToken（7 天）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenVO {

    private String accessToken;

    private String refreshToken;

    /** 用户ID，前端生成 memoryId 使用 */
    private Long userId;

    private String username;
}
