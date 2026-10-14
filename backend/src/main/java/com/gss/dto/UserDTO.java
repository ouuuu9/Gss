package com.gss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增/修改用户请求
 */
@Data
public class UserDTO {

    /** 用户名（新增必填） */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 初始密码（新增必填，修改时忽略） */
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String name;

    @NotBlank(message = "角色不能为空")
    private String role;

    /** 所属公司ID（COMPANY/DRIVER 必填） */
    private Long companyId;

    /** 关联司机ID（DRIVER 角色可指定） */
    private Long driverId;

    private Integer status;
}
