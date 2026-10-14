package com.gss.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * EIR单证实体（eir_order）
 */
@Data
public class EirOrder {

    /** 单证ID */
    private Long id;

    /** 单证编号 */
    private String eirNo;

    /** 业务类型：PI-F提重/PI-E提空/GR-E还空/GR-F还重 */
    private String businessType;

    /** 集装箱号 */
    private String containerNo;

    /** 箱尺寸：20/40 */
    private Integer containerSize;

    /** 箱型：GP/HQ */
    private String containerType;

    /** 作业公司ID */
    private Long companyId;

    /** 派单拖车ID */
    private Long tractorId;

    /** 派单司机ID */
    private Long driverId;

    /** 单证状态：CREATED/DISPATCHED/APPOINTED/GATE_IN/GATE_OUT/DONE/CANCELLED */
    private String status;

    /** 进闸时间 */
    private LocalDateTime gateInTime;

    /** 出闸时间 */
    private LocalDateTime gateOutTime;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 乐观锁前置状态（非数据库字段，不参与 insert/update 显式列）。
     * 状态流转时设置，updateStatus 会追加 AND status = #{expectedStatus} 条件，
     * 用于并发场景下的 CAS 防重（如防止重复派单导致重复计费）。
     */
    private String expectedStatus;
}
