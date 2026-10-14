package com.gss.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单证列表 VO（含公司名、司机名、车牌、缴费状态）
 */
@Data
public class EirOrderVO {

    private Long id;
    private String eirNo;
    private String businessType;
    private String containerNo;
    private Integer containerSize;
    private String containerType;
    private Long companyId;
    /** 作业公司名称 */
    private String companyName;
    private Long tractorId;
    /** 派单车牌号 */
    private String plateNo;
    private Long driverId;
    /** 派单司机姓名 */
    private String driverName;
    private String status;
    /** 派单时间（取消派单回退后清空） */
    private LocalDateTime dispatchTime;
    /** 最近一次预约创建时间 */
    private LocalDateTime apptTime;
    private LocalDateTime gateInTime;
    private LocalDateTime gateOutTime;
    /** 审单完成时间 */
    private LocalDateTime auditTime;
    private String remark;
    /** 缴费状态：null未生成收费/0未缴/1已缴 */
    private Integer payStatus;
    private LocalDateTime createTime;
    /** 最近闸口操作类型：GATE_IN/GATE_OUT/AUDIT（闸口办单台账用） */
    private String opType;
    /** 最近闸口操作时间（取进闸/出闸/审单时间中最新者） */
    private LocalDateTime opTime;
}
