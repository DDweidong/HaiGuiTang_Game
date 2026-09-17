package com.weidong.gamebackend.common.exception;

import com.weidong.gamebackend.common.ErrorCode;
import com.weidong.gamebackend.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 * 把业务异常、参数校验异常、未捕获异常统一转成 Result，保证对外响应格式一致，
 * 避免异常堆栈直接暴露给前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常: 返回业务错误码与提示 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** @Valid 参数校验失败: 取第一条字段错误提示返回 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.PARAM_ERROR.getMessage());
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /** 兜底: 记录完整堆栈用于排查，对外只返回通用提示（含 LLM 调用失败等） */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("未捕获异常", e);
        return Result.error(ErrorCode.SYSTEM_ERROR);
    }
}
