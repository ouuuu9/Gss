package com.gss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建进港预约请求
 */
@Data
public class AppointmentDTO {

    @NotNull(message = "单证不能为空")
    private Long eirId;

    /** 车牌号（可由系统按拖车自动带出） */
    private String plateNo;

    @NotNull(message = "预约日期不能为空")
    private LocalDate apptDate;

    /** 预约时段（如 08:00-10:00） */
    @NotBlank(message = "预约时段不能为空")
    private String timeSlot;

    /** 司机ID（COMPANY 代预约时传入；DRIVER 自助预约由后端自动填充） */
    private Long driverId;
}
