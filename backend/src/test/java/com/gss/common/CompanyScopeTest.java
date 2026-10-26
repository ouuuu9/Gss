package com.gss.common;

import com.gss.security.LoginUser;
import com.gss.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * CompanyScope 角色矩阵单测：钉死数据权限映射规则，
 * 防止改动角色分支时悄悄引入越权。
 */
class CompanyScopeTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private void login(String role, Long companyId, Long driverId) {
        UserContext.set(LoginUser.builder()
                .userId(1L).role(role).companyId(companyId).driverId(driverId).build());
    }

    /** COMPANY：强制本公司，无视前端传的 companyId */
    @Test
    void companyRoleForcedToOwnCompany() {
        login("COMPANY", 2L, null);
        assertEquals(2L, CompanyScope.resolve(99L));

        CompanyScope.Scope scope = CompanyScope.of(99L, 7L);
        assertEquals(2L, scope.companyId());
        assertEquals(7L, scope.driverId());
    }

    /** DRIVER：强制本人司机，companyId 置空 */
    @Test
    void driverRoleForcedToSelf() {
        login("DRIVER", 2L, 5L);
        CompanyScope.Scope scope = CompanyScope.of(99L, 7L);
        assertNull(scope.companyId());
        assertEquals(5L, scope.driverId());
    }

    /** ADMIN：透传请求参数 */
    @Test
    void adminRolePassesThrough() {
        login("ADMIN", null, null);
        assertEquals(99L, CompanyScope.resolve(99L));

        CompanyScope.Scope scope = CompanyScope.of(99L, 7L);
        assertEquals(99L, scope.companyId());
        assertEquals(7L, scope.driverId());
    }

    /** GATE：与 ADMIN 一样透传 */
    @Test
    void gateRolePassesThrough() {
        login("GATE", null, null);
        assertEquals(99L, CompanyScope.resolve(99L));

        CompanyScope.Scope scope = CompanyScope.of(null, null);
        assertNull(scope.companyId());
        assertNull(scope.driverId());
    }
}
