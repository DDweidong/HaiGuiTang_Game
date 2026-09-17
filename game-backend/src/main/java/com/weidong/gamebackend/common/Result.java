package com.weidong.gamebackend.common;

import lombok.Data;

/**
 * 统一响应体。
 * 所有接口（包括 /chat）都返回该结构，前端只需判断 code 是否成功、从 data 取业务数据，
 * 避免各接口返回格式不一致。
 */
@Data
public class Result<T> {

    /** 业务状态码，0 表示成功，其余见 {@link ErrorCode} */
    private int code;

    /** 提示信息，成功时为 "ok"，失败时为可展示给用户的原因 */
    private String message;

    /** 业务数据 */
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.SUCCESS.getCode());
        result.setMessage(ErrorCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
