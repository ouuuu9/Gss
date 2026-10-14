package com.gss.common;

/**
 * 业务异常：Service 层状态机校验、业务规则校验失败时抛出，
 * 由全局异常处理器转换为统一响应格式
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
