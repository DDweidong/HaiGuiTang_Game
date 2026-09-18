package com.weidong.gamebackend.security;

/**
 * 当前登录用户，作为 SecurityContext 中 Authentication 的 principal。
 * 由 JwtAuthenticationFilter 解析 access token 后构建，业务代码通过 SecurityUtil 获取。
 */
public record LoginUser(Long id, String username) {
}
