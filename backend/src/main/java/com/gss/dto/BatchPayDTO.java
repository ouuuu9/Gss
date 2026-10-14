package com.gss.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量缴费请求
 */
@Data
public class BatchPayDTO {

    @NotEmpty(message = "请至少选择一条收费记录")
    private List<Long> ids;
}
