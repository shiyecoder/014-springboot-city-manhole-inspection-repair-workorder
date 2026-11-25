package com.city.manhole.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：业务异常回 400，其余回 500，并且不向调用方暴露堆栈
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValid(Exception e) {
        String msg = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException m && m.getBindingResult().getFieldError() != null) {
            msg = m.getBindingResult().getFieldError().getDefaultMessage();
        } else if (e instanceof BindException b && b.getFieldError() != null) {
            msg = b.getFieldError().getDefaultMessage();
        }
        return Result.fail(400, msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleBody(HttpMessageNotReadableException e) {
        return Result.fail(400, "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统繁忙，请稍后重试");
    }
}
