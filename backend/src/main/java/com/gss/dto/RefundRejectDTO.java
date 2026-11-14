package com.gss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 驳回退款申请入参
 */
@Data
public class RefundRejectDTO {

    /** 驳回原因 */
    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 200, message = "驳回原因不能超过200字")
    private String reason;
}
