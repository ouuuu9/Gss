package com.gss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 录入/修改 EIR 单证请求
 */
@Data
public class EirDTO {

    /** 单证编号（录入时为空则后端自动生成） */
    private String eirNo;

    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @NotBlank(message = "集装箱号不能为空")
    private String containerNo;

    @NotNull(message = "箱尺寸不能为空")
    private Integer containerSize;

    @NotBlank(message = "箱型不能为空")
    private String containerType;

    @NotNull(message = "作业公司不能为空")
    private Long companyId;

    private String remark;
}
