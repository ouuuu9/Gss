package com.gss.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 大屏数据刷新注解：标注在会影响数据大屏指标的写操作上
 * （进出闸、缴费、预约、派单等），方法执行成功后由切面
 * 触发 WebSocket 广播，将最新统计数据实时推送到大屏。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RefreshScreen {
}
