package com.gss.aspect;

import com.gss.websocket.ScreenWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 大屏实时刷新切面：标注 @RefreshScreen 的写操作执行成功后，
 * 立即向全部在线大屏广播最新统计数据。
 * 推送异常不影响业务主流程（广播本身只做查询与发送）。
 */
@Slf4j
@Aspect
@Component
@Order(2)
@RequiredArgsConstructor
public class ScreenRefreshAspect {

    private final ScreenWebSocketHandler screenWebSocketHandler;

    @AfterReturning("@annotation(com.gss.annotation.RefreshScreen)")
    public void afterSuccess() {
        try {
            screenWebSocketHandler.broadcast();
        } catch (Exception e) {
            log.error("大屏数据推送失败", e);
        }
    }
}
