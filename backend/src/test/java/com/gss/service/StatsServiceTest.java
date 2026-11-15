package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.dto.StatsQueryDTO;
import com.gss.mapper.AppointmentStatsMapper;
import com.gss.security.LoginUser;
import com.gss.vo.StatsResultVO;
import com.gss.vo.StatsRowVO;
import com.gss.vo.StatsSummaryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M4 统计口径单测：比率分母、除零、入参归一、COMPANY 数据范围。
 */
@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock private AppointmentStatsMapper statsMapper;

    private StatsService service;

    @BeforeEach
    void setUp() {
        service = new StatsService(statsMapper);
    }

    private LoginUser admin() {
        return LoginUser.builder().userId(1L).role("ADMIN").build();
    }

    private StatsSummaryVO summary(long total, long cancelled, long noShow, long released,
                                   long pass, long fail, long onTime) {
        StatsSummaryVO s = new StatsSummaryVO();
        s.setTotal(total);
        s.setCancelled(cancelled);
        s.setNoShow(noShow);
        s.setReleased(released);
        s.setPassCount(pass);
        s.setFailCount(fail);
        s.setOnTimeCount(onTime);
        return s;
    }

    /** 验收 3/5：失约率 3/(20-4)=18.75%，准点率 10/12=83.33%，放行率 12/16=75% */
    @Test
    void ratesFollowDefinedDenominators() {
        when(statsMapper.summary(any())).thenReturn(summary(20, 4, 3, 12, 12, 3, 10));

        StatsResultVO result = service.stats(new StatsQueryDTO(), admin());

        assertEquals(18.75, result.getSummary().getNoShowRate());
        assertEquals(83.33, result.getSummary().getPunctualRate());
        assertEquals(75.0, result.getSummary().getReleaseRate());
    }

    /** 分母为 0 不抛异常，返回 0 */
    @Test
    void zeroDenominatorReturnsZero() {
        when(statsMapper.summary(any())).thenReturn(summary(0, 0, 0, 0, 0, 0, 0));

        StatsSummaryVO s = service.stats(new StatsQueryDTO(), admin()).getSummary();

        assertEquals(0.0, s.getNoShowRate());
        assertEquals(0.0, s.getPunctualRate());
        assertEquals(0.0, s.getReleaseRate());
    }

    /** 全部取消时失约率分母为 0 */
    @Test
    void allCancelledMeansZeroRate() {
        when(statsMapper.summary(any())).thenReturn(summary(5, 5, 0, 0, 0, 0, 0));

        assertEquals(0.0, service.stats(new StatsQueryDTO(), admin()).getSummary().getNoShowRate());
    }

    /** 分组明细同样计算比率 */
    @Test
    void groupedRowsAlsoHaveRates() {
        StatsRowVO row = new StatsRowVO();
        row.setGroupKey("GATE-1 / LANE-1");
        row.setTotal(8L);
        row.setCancelled(0L);
        row.setNoShow(1L);
        row.setReleased(7L);
        row.setPassCount(7L);
        row.setOnTimeCount(7L);
        when(statsMapper.summary(any())).thenReturn(summary(8, 0, 1, 7, 7, 0, 7));
        when(statsMapper.grouped(any())).thenReturn(List.of(row));

        StatsQueryDTO q = new StatsQueryDTO();
        q.setGroupBy("lane");
        StatsResultVO result = service.stats(q, admin());

        assertEquals("LANE", q.getGroupBy());
        assertEquals(12.5, result.getRows().get(0).getNoShowRate());
        assertEquals(100.0, result.getRows().get(0).getPunctualRate());
    }

    /** 非法分组维度直接拒绝 */
    @Test
    void invalidGroupByRejected() {
        StatsQueryDTO q = new StatsQueryDTO();
        q.setGroupBy("FOO");

        assertThrows(BusinessException.class, () -> service.stats(q, admin()));
        verify(statsMapper, never()).summary(any());
    }

    /** 日期反了自动交换；缺省取今天 */
    @Test
    void datesNormalized() {
        StatsQueryDTO q = new StatsQueryDTO();
        q.setStartDate(LocalDate.of(2026, 11, 15));
        q.setEndDate(LocalDate.of(2026, 11, 10));
        when(statsMapper.summary(any())).thenReturn(summary(0, 0, 0, 0, 0, 0, 0));

        service.stats(q, admin());

        assertEquals(LocalDate.of(2026, 11, 10), q.getStartDate());
        assertEquals(LocalDate.of(2026, 11, 15), q.getEndDate());
    }

    /** COMPANY 角色强制只看本公司 */
    @Test
    void companyScopeForced() {
        StatsQueryDTO q = new StatsQueryDTO();
        q.setCompanyId(99L);
        LoginUser company = LoginUser.builder().userId(2L).role("COMPANY").companyId(5L).build();
        when(statsMapper.summary(any())).thenReturn(summary(0, 0, 0, 0, 0, 0, 0));

        service.stats(q, company);

        assertEquals(5L, q.getCompanyId());
    }
}