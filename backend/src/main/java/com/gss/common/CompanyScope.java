package com.gss.common;

import com.gss.security.LoginUser;
import com.gss.security.UserContext;

/**
 * 公司数据范围：COMPANY 角色的查询/写入公司条件统一在此强制解析，
 * 各 Controller 不再各自实现，避免规则改一处漏一处。
 */
public final class CompanyScope {

    private CompanyScope() {
    }

    /**
     * 解析公司过滤条件：COMPANY 强制返回本人所属公司（前端传值无效），
     * 其余角色透传请求参数
     */
    public static Long resolve(Long paramCompanyId) {
        LoginUser user = UserContext.require();
        if (Constants.ROLE_COMPANY.equals(user.getRole())) {
            return user.getCompanyId();
        }
        return paramCompanyId;
    }

    /**
     * 数据范围（公司 + 司机双维度）
     *
     * @param companyId 生效的公司过滤；null 表示不限公司
     * @param driverId  生效的司机过滤；null 表示不限司机
     */
    public record Scope(Long companyId, Long driverId) {
    }

    /**
     * 解析双维度数据范围：DRIVER 强制本人司机且不限公司；
     * COMPANY 强制本公司；其余角色透传请求参数
     */
    public static Scope of(Long paramCompanyId, Long paramDriverId) {
        LoginUser user = UserContext.require();
        if (Constants.ROLE_DRIVER.equals(user.getRole())) {
            return new Scope(null, user.getDriverId());
        }
        if (Constants.ROLE_COMPANY.equals(user.getRole())) {
            return new Scope(user.getCompanyId(), paramDriverId);
        }
        return new Scope(paramCompanyId, paramDriverId);
    }
}
