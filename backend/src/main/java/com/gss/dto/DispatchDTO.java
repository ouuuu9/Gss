package com.gss.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 派单请求：派给指定司机与拖车
 */
@Data
public class DispatchDTO {

    @NotNull(message = "司机不能为空")
    private Long driverId;

    @NotNull(message = "拖车不能为空")
    private Long tractorId;
}
