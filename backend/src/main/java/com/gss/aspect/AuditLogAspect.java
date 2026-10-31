package com.gss.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gss.annotation.AuditLog;
import com.gss.entity.AuditLogEntity;
import com.gss.mapper.AuditLogMapper;
import com.gss.security.LoginUser;
import com.gss.security.UserContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 操作审计日志环绕切面：
 * 拦截所有标注 @AuditLog 的方法，成功/失败均落库，记录操作人、请求信息、
 * 脱敏后的参数、客户端IP、结果与耗时。审计异常自行捕获，不阻断业务主流程。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    /** 参数与错误信息最大落库长度，与 audit_log 表字段定义一致 */
    private static final int MAX_TEXT_LENGTH = 500;

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        Throwable error = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                saveLog(joinPoint, auditLog, error, System.currentTimeMillis() - start);
            } catch (Exception e) {
                // 审计失败不得阻断主业务流程，仅记录切面自身错误
                log.error("保存操作审计日志失败", e);
            }
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, AuditLog auditLog,
                         Throwable error, long costMs) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setOperation(auditLog.value());

        LoginUser user = UserContext.get();
        if (user != null) {
            entity.setUsername(user.getUsername());
            entity.setRole(user.getRole());
        }

        HttpServletRequest request = currentRequest();
        if (request != null) {
            entity.setMethod(request.getMethod());
            entity.setUri(request.getRequestURI());
            entity.setIp(clientIp(request));
        }

        entity.setParams(toMaskedParams(joinPoint.getArgs()));

        if (error == null) {
            entity.setSuccess(1);
        } else {
            entity.setSuccess(0);
            entity.setErrorMsg(truncate(error.getMessage()));
        }
        entity.setCostMs((int) costMs);

        auditLogMapper.insert(entity);
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * 序列化请求参数为 JSON 并脱敏：顶层字段名包含 password 一律替换为 ***，
     * Servlet 请求/响应对象等不可序列化参数直接跳过
     */
    private String toMaskedParams(Object[] args) {
        List<Object> serializable = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null || arg instanceof ServletRequest || arg instanceof ServletResponse) {
                continue;
            }
            serializable.add(arg);
        }
        if (serializable.isEmpty()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(serializable));
            maskPasswords(node);
            return truncate(objectMapper.writeValueAsString(node));
        } catch (Exception e) {
            // 兜底：序列化失败时记录参数个数，避免敏感对象 toString 泄露
            return truncate("[args=" + serializable.size() + ", 序列化失败]");
        }
    }

    private void maskPasswords(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (field.getKey().toLowerCase().contains("password")) {
                    field.setValue(objectMapper.getNodeFactory().textNode("***"));
                } else {
                    maskPasswords(field.getValue());
                }
            }
        } else if (node.isArray()) {
            node.forEach(this::maskPasswords);
        }
    }

    /**
     * 客户端IP：优先取反向代理透传的 X-Forwarded-For 首段
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        if (text.length() <= MAX_TEXT_LENGTH) {
            return text;
        }
        // 超长内容按列定义截断，避免落库失败
        return text.substring(0, MAX_TEXT_LENGTH);
    }
}
