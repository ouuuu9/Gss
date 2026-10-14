package com.gss.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等注解：标注在需要防重复提交的写操作上。
 * 切面对「用户 + 方法 + 参数」计算请求指纹，TTL 窗口内的
 * 重复请求直接拒绝，防止网络重试/双击/F5 造成重复创建、重复缴费。
 * 与底层状态机 CAS 形成双保险：幂等拦截返回友好提示，CAS 兜底并发。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 去重窗口（秒），窗口内相同指纹的请求视为重复提交
     */
    int ttlSeconds() default 3;
}
