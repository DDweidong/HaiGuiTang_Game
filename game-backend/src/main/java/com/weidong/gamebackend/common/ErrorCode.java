package com.weidong.gamebackend.common;

import lombok.Getter;

/**
 * 统一错误码。
 * 设计约定: code=0 表示成功；业务错误按 4xxxx（客户端问题）/ 5xxxx（服务端问题）分段，
 * 便于前端按段做统一处理（如 40100 跳登录）。
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAM_ERROR(40000, "请求参数错误"),
    USERNAME_EXISTS(40001, "用户名已存在"),
    LOGIN_FAILED(40002, "用户名或密码错误"),
    TOKEN_INVALID(40003, "登录凭证无效，请重新登录"),
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    NOT_FOUND(40400, "资源不存在"),
    FORBIDDEN(40300, "无权访问"),
    SYSTEM_ERROR(50000, "系统繁忙，请稍后再试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
