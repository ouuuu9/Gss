package com.gss.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增/修改运输公司请求
 */
@Data
public class CompanyDTO {

    @NotBlank(message = "公司编码不能为空")
    private String code;

    @NotBlank(message = "公司名称不能为空")
    private String name;

    private String contact;

    private String phone;

    private Integer status;
}
