package com.gss.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录用户信息（从 JWT 解析后放入线程上下文）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    /** 用户ID */
    private Long userId;

    /** 登录用户名 */
    private String username;

    /** 真实姓名 */
    private String name;

    /** 角色 */
    private String role;

    /** 所属公司ID（码头端用户为空） */
    private Long companyId;

    /** 关联司机ID（仅 DRIVER 角色） */
    private Long driverId;
}
