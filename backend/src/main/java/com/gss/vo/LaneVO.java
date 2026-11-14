package com.gss.vo;

import lombok.Data;

import java.util.List;

/**
 * 车道列表 VO（M3）：基础信息 + 各时段容量概要
 */
@Data
public class LaneVO {

    private Long id;
    private String gateNo;
    private String laneNo;
    private Integer capacityPerSlot;
    private String status;
    private String remark;
    /** 容量概要文字，如「8 时段已配置」 */
    private String capacitySummary;
    /** 该车道未完成（ACTIVE/VERIFIED）的预约数，停用前提示用 */
    private Long activeAppointments;
    /** 各时段容量明细（列表页可展开） */
    private List<CapacityCell> cells;

    @Data
    public static class CapacityCell {
        private String timeSlot;
        private Integer capacity;
        private Integer bookedCount;
        private Integer remain;
    }
}
