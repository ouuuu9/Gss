package com.gss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 办单审核驳回请求（M1）：驳回原因必填，长度 1-200 字。
 */
@Data
public class EirRejectDTO {

    /** 驳回原因（必填，前端展示给客户） */
    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 200, message = "驳回原因不能超过 200 字")
    private String rejectReason;
}
