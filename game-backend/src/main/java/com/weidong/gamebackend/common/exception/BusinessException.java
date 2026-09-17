package com.weidong.gamebackend.common.exception;

import com.weidong.gamebackend.common.ErrorCode;
import lombok.Getter;

/**
 * 业务异常。
 * 业务代码中主动抛出的异常，由 {@link GlobalExceptionHandler} 统一转成 Result 返回，
 * 不再需要每个接口自己 try-catch 拼错误响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
