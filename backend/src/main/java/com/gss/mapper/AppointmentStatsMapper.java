package com.gss.mapper;

import com.gss.dto.StatsQueryDTO;
import com.gss.vo.StatsRowVO;
import com.gss.vo.StatsSummaryVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 预约执行统计 Mapper（M4）。SQL 全部在 XML 中，按 appt_date 过滤走索引。
 */
public interface AppointmentStatsMapper {

    /** 汇总（不分组） */
    StatsSummaryVO summary(@Param("q") StatsQueryDTO query);

    /**
     * 分组明细：LANE / SLOT / STATUS / COMPANY。
     * 调用方保证 query.groupBy 合法，且 SQL 只按该列分组。
     */
    List<StatsRowVO> grouped(@Param("q") StatsQueryDTO query);
}
