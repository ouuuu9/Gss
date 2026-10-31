package com.gss.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gss.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据大屏 WebSocket 处理器：
 * 维护全部在线大屏会话，业务变更（@RefreshScreen）或 30 秒心跳
 * 触发时查询最新统计数据并广播。连接建立即推送一次全量数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScreenWebSocketHandler extends TextWebSocketHandler {

    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    /** sessionId -> 会话，线程安全 */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        sendTo(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session.getId());
    }

    /**
     * 向全部在线大屏广播最新统计数据。
     * 发送失败或已关闭的会话会被剔除。
     */
    public void broadcast() {
        if (sessions.isEmpty()) {
            return;
        }
        String payload = buildPayload();
        if (payload == null) {
            return;
        }
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions.values()) {
            // 发送失败说明连接已不可用，直接剔除，避免下次广播继续无效发送
            if (!trySend(session, message)) {
                sessions.remove(session.getId());
            }
        }
    }

    /**
     * 30 秒定时全量推送：兜底保证大屏数据新鲜度，兼作连接保活
     */
    @Scheduled(fixedDelay = 30000)
    public void heartbeat() {
        broadcast();
    }

    private void sendTo(WebSocketSession session) {
        String payload = buildPayload();
        if (payload != null) {
            trySend(session, new TextMessage(payload));
        }
    }

    private String buildPayload() {
        try {
            return objectMapper.writeValueAsString(dashboardService.screen());
        } catch (Exception e) {
            log.error("序列化大屏数据失败", e);
            return null;
        }
    }

    /**
     * WebSocketSession 非线程安全，同一会话的并发发送需串行化
     */
    private boolean trySend(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) {
            return false;
        }
        try {
            synchronized (session) {
                session.sendMessage(message);
            }
            return true;
        } catch (IOException e) {
            log.warn("大屏推送失败，剔除会话 {}", session.getId());
            return false;
        }
    }
}
