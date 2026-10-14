package com.gss.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约列表 VO（含单证编号、司机姓名、箱号等）
 */
@Data
public class AppointmentVO {

    private Long id;
    private Long eirId;
    /** 单证编号 */
    private String eirNo;
    /** 业务类型 */
    private String businessType;
    /** 集装箱号 */
    private String containerNo;
    private String plateNo;
    private LocalDate apptDate;
    /** 预约时段（如 08:00-10:00） */
    private String timeSlot;
    private String status;
    private Long driverId;
    /** 司机姓名 */
    private String driverName;
    private Long companyId;
    /** 所属公司名称 */
    private String companyName;
    private LocalDateTime createTime;
}
