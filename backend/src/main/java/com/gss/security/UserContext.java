package com.gss.security;

import com.gss.common.BusinessException;

/**
 * 当前登录用户上下文（基于 ThreadLocal），
 * JwtInterceptor 认证通过后写入，请求结束后清理
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 获取当前用户，未登录抛出业务异常
     */
    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException("未获取到登录信息，请重新登录");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
