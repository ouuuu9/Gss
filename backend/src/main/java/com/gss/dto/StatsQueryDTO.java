package com.gss.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 预约执行统计查询条件（M4）。
 * 全部口径按 appointment.appt_date 落在 [startDate, endDate] 内统计。
 */
@Data
public class StatsQueryDTO {

    private LocalDate startDate;

    private LocalDate endDate;

    /** 车道筛选 */
    private Long laneId;

    /** 时段筛选（如 08:00-10:00） */
    private String timeSlot;

    /** 客户（作业公司）筛选 */
    private Long companyId;

    /** 分组维度：LANE / SLOT / STATUS / COMPANY；为空表示不分组 */
    private String groupBy;
}
