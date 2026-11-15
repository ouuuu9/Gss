package com.gss.vo;

import lombok.Data;

/**
 * 分组统计行（M4）：分组键 + 与汇总相同的 6 个量与 3 个比率。
 */
@Data
public class StatsRowVO {

    /** 分组键：车道「GATE-1 / LANE-1」、时段、状态、客户名 */
    private String groupKey;

    private Long total = 0L;
    private Long cancelled = 0L;
    private Long noShow = 0L;
    private Long released = 0L;
    private Long passCount = 0L;
    private Long failCount = 0L;
    private Long onTimeCount = 0L;

    private Double noShowRate = 0.0;
    private Double punctualRate = 0.0;
    private Double releaseRate = 0.0;
}
