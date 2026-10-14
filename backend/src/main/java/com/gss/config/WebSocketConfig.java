package com.gss.config;

import com.gss.security.JwtUtil;
import com.gss.security.LoginUser;
import com.gss.websocket.ScreenWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * WebSocket 配置：注册大屏推送端点 /ws/screen，
 * 握手阶段校验 query 中的 JWT token，非法 token 拒绝升级连接。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ScreenWebSocketHandler screenWebSocketHandler;
    private final JwtUtil jwtUtil;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(screenWebSocketHandler, "/ws/screen")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        String token = UriComponentsBuilder.fromUri(request.getURI())
                                .build()
                                .getQueryParams()
                                .getFirst("token");
                        if (token == null || token.isBlank()) {
                            response.setStatusCode(HttpStatus.UNAUTHORIZED);
                            return false;
                        }
                        try {
                            LoginUser user = jwtUtil.parseToken(token);
                            // 大屏仅 ADMIN / GATE 可连接，公司/司机无权查看全码头实时数据
                            if (user != null && ("ADMIN".equals(user.getRole()) || "GATE".equals(user.getRole()))) {
                                return true;
                            }
                        } catch (Exception ignored) {
                            // token 非法，按未认证处理
                        }
                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                        return false;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler wsHandler, Exception exception) {
                        // no-op
                    }
                })
                .setAllowedOrigins("*");
    }
}
