package com.recplatform.common.result;

import com.recplatform.common.util.TraceIdUtil;
import lombok.Data;

import java.io.Serializable;

/**
 * Generic unified API response wrapper.
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;
    private long timestamp;
    private String traceId;

    private Result() {
        this.timestamp = System.currentTimeMillis();
        this.traceId = TraceIdUtil.getTraceId();
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return success(data, ResultCode.SUCCESS.getMessage());
    }

    public static <T> Result<T> success(T data, String message) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static Result<Void> fail(ResultCode code) {
        Result<Void> result = new Result<>();
        result.setCode(code.getCode());
        result.setMessage(code.getMessage());
        return result;
    }

    public static Result<Void> fail(int code, String message) {
        Result<Void> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * Typed error response so callers expecting a specific data type can return
     * a failure without a generics mismatch.
     */
    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
