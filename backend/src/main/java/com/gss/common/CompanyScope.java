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
}
