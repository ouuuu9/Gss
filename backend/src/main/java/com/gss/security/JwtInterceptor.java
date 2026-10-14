package com.gss.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gss.common.Result;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * JWT 认证与角色校验拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 非 Controller 方法（静态资源等）直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 1. 取 Authorization 请求头
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, 401, "未登录或登录已过期");
            return false;
        }

        // 2. 解析 JWT
        String token = authHeader.substring(7);
        LoginUser loginUser;
        try {
            loginUser = jwtUtil.parseToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 解析失败：{}", e.getMessage());
            writeError(response, 401, "登录凭证无效或已过期，请重新登录");
            return false;
        }

        // 3. 放入线程上下文
        UserContext.set(loginUser);

        // 4. 角色校验（方法级注解优先于类级注解）
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null) {
            boolean allowed = Arrays.asList(requireRole.value()).contains(loginUser.getRole());
            if (!allowed) {
                writeError(response, 403, "无权限执行该操作");
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束清理线程上下文，防止内存泄漏
        UserContext.clear();
    }

    /**
     * 输出 JSON 格式错误信息
     */
    private void writeError(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, msg)));
    }
}
