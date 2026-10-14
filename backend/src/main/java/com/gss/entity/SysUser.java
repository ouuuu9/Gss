package com.gss.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体（sys_user）
 */
@Data
public class SysUser {

    /** 用户ID */
    private Long id;

    /** 登录用户名 */
    private String username;

    /** 登录密码（BCrypt加密） */
    private String password;

    /** 真实姓名 */
    private String name;

    /** 角色：ADMIN/GATE/COMPANY/DRIVER */
    private String role;

    /** 所属运输公司ID（码头端用户为空） */
    private Long companyId;

    /** 关联司机ID（仅DRIVER角色） */
    private Long driverId;

    /** 状态：1正常 0禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
