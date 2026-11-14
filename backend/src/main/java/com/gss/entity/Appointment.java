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

    /** 状态：ACTIVE有效/VERIFIED已核验/RELEASED已放行/CANCELLED已取消/NO_SHOW失约 */
    private String status;

    /** 预约凭证号（服务端生成，格式 APT + yyyyMMdd + 6 位当日序列） */
    private String voucherNo;

    /** 核验时间（闸口核验通过时写入；为空表示未核验） */
    private LocalDateTime verifyTime;

    /** 放行时间（出闸释放时写入） */
    private LocalDateTime releaseTime;

    /** 司机ID */
    private Long driverId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
