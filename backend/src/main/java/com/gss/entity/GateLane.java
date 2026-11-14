package com.gss.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 闸口车道（gate_lane，M3）：每个车道独立配置单时段容量。
 */
@Data
public class GateLane {

    private Long id;

    /** 闸口号，如 GATE-1 */
    private String gateNo;

    /** 车道号，如 LANE-1 */
    private String laneNo;

    /** 单时段默认容量 */
    private Integer capacityPerSlot;

    /** 状态：ENABLED启用 / DISABLED停用 */
    private String status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
