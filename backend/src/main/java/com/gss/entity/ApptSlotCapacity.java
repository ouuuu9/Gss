package com.gss.entity;

import lombok.Data;

import java.time.LocalDate;

/**
 * 预约时段容量实体（appt_slot_capacity）
 */
@Data
public class ApptSlotCapacity {

    /** 容量ID */
    private Long id;

    /** 预约日期 */
    private LocalDate apptDate;

    /** 预约时段（如 08:00-10:00） */
    private String timeSlot;

    /** 时段最大预约数 */
    private Integer capacity;

    /** 已预约数 */
    private Integer bookedCount;
}
