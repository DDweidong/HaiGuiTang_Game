package com.weidong.gamebackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weidong.gamebackend.common.ErrorCode;
import com.weidong.gamebackend.common.Result;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 把认证/授权失败写成统一 Result JSON。
 * Security 过滤器链中的异常不走 MVC 的 GlobalExceptionHandler，需自行写响应。
 */
final class SecurityResponseUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SecurityResponseUtil() {
    }

    static void write(HttpServletResponse response, int httpStatus, ErrorCode errorCode) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(MAPPER.writeValueAsString(Result.error(errorCode)));
    }
}
