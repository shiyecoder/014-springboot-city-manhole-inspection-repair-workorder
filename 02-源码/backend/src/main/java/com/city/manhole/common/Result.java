package com.city.manhole.common;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

/**
 * 统一响应结构：业务码 + 提示 + 数据 + 请求编号
 */
@Data
public class Result<T> implements Serializable {

    private int code;
    private String message;
    private T data;
    private String traceId;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "操作成功";
        r.data = data;
        r.traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(String message, T data) {
        Result<T> r = ok(data);
        r.message = message;
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return r;
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }
}
