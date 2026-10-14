package com.gss.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量派单请求：将多单证派给同一位司机与同一辆拖车
 */
@Data
public class BatchDispatchDTO {

    @NotEmpty(message = "请选择要派单的单证")
    private List<Long> ids;

    @NotNull(message = "司机不能为空")
    private Long driverId;

    @NotNull(message = "拖车不能为空")
    private Long tractorId;
}
