package com.gss.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：把异常统一转换为 Result 格式
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.gss.controller")
public class GlobalExceptionHandler {

    /**
     * 业务异常（状态机校验失败、拦截等）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{}]：{}", request.getRequestURI(), e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * @RequestBody 参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return Result.error(400, msg);
    }

    /**
     * 表单参数绑定校验失败
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return Result.error(400, msg);
    }

    /**
     * 非法参数
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArg(IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    }

    /**
     * 静态资源未找到（Spring Boot 3.2）：返回 404，不走兜底
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public Result<Void> handleNoResource(org.springframework.web.servlet.resource.NoResourceFoundException e, HttpServletRequest request) {
        log.warn("资源未找到 [{}]：{}", request.getRequestURI(), e.getMessage());
        return Result.error(404, "资源不存在");
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{}]", request.getRequestURI(), e);
        return Result.error(500, "系统繁忙，请联系管理员");
    }
}
