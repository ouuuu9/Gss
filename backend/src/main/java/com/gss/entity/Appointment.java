package com.gss.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 进港预约实体（appointment）
 */
@Data
public class Appointment {

    /** 预约ID */
    private Long id;

    /** 关联单证ID */
    private Long eirId;

    /** 预约车牌号 */
    private String plateNo;

    /** 预约日期 */
    private LocalDate apptDate;

    /** 预约时段（如 08:00-10:00） */
    private String timeSlot;

    /** 状态：ACTIVE有效/CANCELLED已取消 */
    private String status;

    /** 司机ID */
    private Long driverId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
