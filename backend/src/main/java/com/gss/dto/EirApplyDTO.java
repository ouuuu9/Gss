package com.gss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户提交办单申请（M1）。
 * 作业公司由登录身份强制决定，不接受前端传入，避免越权替其他公司申请。
 */
@Data
public class EirApplyDTO {

    /** 业务类型：PI-F提重/PI-E提空/GR-E还空/GR-F还重 */
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    /** 集装箱号 */
    @NotBlank(message = "集装箱号不能为空")
    private String containerNo;

    /** 箱尺寸：20/40 */
    @NotNull(message = "箱尺寸不能为空")
    private Integer containerSize;

    /** 箱型：GP/HQ */
    @NotBlank(message = "箱型不能为空")
    private String containerType;

    /** 备注 */
    private String remark;
}
