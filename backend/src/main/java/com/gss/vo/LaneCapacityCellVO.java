package com.gss.vo;

import lombok.Data;

/**
 * 车道×时段容量格子（M3 预约弹窗/配额页矩阵）
 */
@Data
public class LaneCapacityCellVO {

    private Long laneId;
    private String gateNo;
    private String laneNo;
    private String timeSlot;
    private Integer capacity;
    private Integer bookedCount;
    private Integer remain;
}
