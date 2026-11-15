package com.gss.vo;

import lombok.Data;

/**
 * 预约执行统计汇总（M4）：6 个基础量 + 3 个比率。
 *
 * 口径：
 * - 预约总数统计区间内全部预约（不限状态）
 * - 失约率 = NO_SHOW / (总数 - 取消)，取消不计失信
 * - 准点率 = 准时核验数 / 核验通过数（verify_time 时分 <= 时段起点）
 * - 放行率 = RELEASED / (总数 - 取消)
 * - 核验通过数排除 gate_check_log 中备注含「重复核验」的记录
 */
@Data
public class StatsSummaryVO {

    private Long total = 0L;
    private Long cancelled = 0L;
    private Long noShow = 0L;
    private Long released = 0L;
    /** 核验通过数（已排除重复核验） */
    private Long passCount = 0L;
    private Long failCount = 0L;
    /** 准时核验的预约数 */
    private Long onTimeCount = 0L;

    /** 失约率（%） */
    private Double noShowRate = 0.0;
    /** 准点率（%） */
    private Double punctualRate = 0.0;
    /** 放行率（%） */
    private Double releaseRate = 0.0;
}
