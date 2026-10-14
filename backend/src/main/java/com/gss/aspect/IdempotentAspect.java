package com.gss.aspect;

import com.gss.annotation.Idempotent;
import com.gss.common.BusinessException;
import com.gss.security.LoginUser;
import com.gss.security.UserContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口幂等切面：对标注 @Idempotent 的方法计算请求指纹
 * （用户ID + 方法签名 + 参数摘要的 MD5），TTL 窗口内重复指纹直接拒绝。
 * Order(1) 保证最先执行：重复请求在入口即被拦截，
 * 不会产生审计日志，也不会触发大屏刷新。
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class IdempotentAspect {

    /** 请求指纹 -> 过期时间戳（毫秒） */
    private final Map<String, Long> recentRequests = new ConcurrentHashMap<>();

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        long now = System.currentTimeMillis();
        // 惰性清理过期指纹，防止长时间运行后 Map 膨胀
        if (recentRequests.size() > 1000) {
            recentRequests.entrySet().removeIf(e -> e.getValue() < now);
        }

        String fingerprint = fingerprint(joinPoint);
        long expireAt = now + idempotent.ttlSeconds() * 1000L;
        Long prev = recentRequests.putIfAbsent(fingerprint, expireAt);
        if (prev != null) {
            if (prev > now) {
                // 窗口内重复请求，直接拒绝
                throw new BusinessException("请求正在处理中，请勿重复提交");
            }
            // 旧指纹已过期，刷新过期时间，开启新的去重窗口
            recentRequests.put(fingerprint, expireAt);
        }
        return joinPoint.proceed();
    }

    private String fingerprint(ProceedingJoinPoint joinPoint) {
        LoginUser user = UserContext.get();
        StringBuilder sb = new StringBuilder();
        sb.append(user != null ? user.getUserId() : "anonymous").append('|');
        sb.append(joinPoint.getSignature().toShortString()).append('|');
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof ServletRequest || arg instanceof ServletResponse) {
                continue;
            }
            sb.append(arg == null ? "null" : arg.toString()).append(',');
        }
        return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
