package com.gss.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计日志注解：标注在 Controller 写操作方法上，
 * 由 AuditLogAspect 环绕拦截自动记录"谁/何时/从哪个IP/干了什么/结果如何"。
 * 审计日志落库失败不阻断业务主流程。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * 操作描述（如"创建进港预约"）
     */
    String value();
}
