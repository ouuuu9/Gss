package com.gss.vo;

import lombok.Data;

import java.util.List;

/**
 * 统计接口返回：汇总 + 分组明细（M4）
 */
@Data
public class StatsResultVO {

    private StatsSummaryVO summary;

    private List<StatsRowVO> rows;
}
