package com.gss.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 车道新增/修改请求（M3）
 */
@Data
public class LaneDTO {

    @NotBlank(message = "闸口号不能为空")
    private String gateNo;

    @NotBlank(message = "车道号不能为空")
    private String laneNo;

    @NotNull(message = "单时段容量不能为空")
    @Min(value = 0, message = "容量不能为负数")
    private Integer capacityPerSlot;

    private String remark;
}
