package com.gss.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审单请求
 */
@Data
public class AuditDTO {

    @NotNull(message = "单证ID不能为空")
    private Long eirId;
}
