package com.gss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增/修改司机请求
 */
@Data
public class DriverDTO {

    @NotBlank(message = "司机姓名不能为空")
    private String name;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String licenseNo;

    /** 绑定拖车ID */
    private Long tractorId;

    @NotNull(message = "所属公司不能为空")
    private Long companyId;

    private Integer status;
}
