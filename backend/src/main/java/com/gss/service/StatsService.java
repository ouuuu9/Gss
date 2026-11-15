package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.common.ExcelExporter;
import com.gss.dto.StatsQueryDTO;
import com.gss.mapper.AppointmentStatsMapper;
import com.gss.security.LoginUser;
import com.gss.vo.StatsResultVO;
import com.gss.vo.StatsRowVO;
import com.gss.vo.StatsSummaryVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 预约执行统计服务（M4）。
 *
 * 口径（与任务书一致，改分母前必须先对齐）：
 * 1. 预约总数 = 区间内全部预约（不限状态）
 * 2. 失约率 = NO_SHOW / (总数 - 取消)          —— 主动取消不算失信，不进分母
 * 3. 准点率 = 准时核验数 / 核验通过数           —— verify_time 时分 <= 时段起点算准时
 * 4. 放行率 = RELEASED / (总数 - 取消)
 * 5. 核验通过数排除 gate_check_log 备注含「重复核验」的 PASS
 * 6. 所有比率分母为 0 时返回 0
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final Set<String> GROUP_BY = Set.of("LANE", "SLOT", "STATUS", "COMPANY");

    private final AppointmentStatsMapper statsMapper;

    /** 汇总 + 可选分组明细 */
    @Transactional(readOnly = true)
    public StatsResultVO stats(StatsQueryDTO query, LoginUser operator) {
        normalize(query, operator);

        StatsSummaryVO summary = statsMapper.summary(query);
        if (summary == null) {
            summary = new StatsSummaryVO();
        }
        fillRates(summary);

        List<StatsRowVO> rows = new ArrayList<>();
        if (StringUtils.hasText(query.getGroupBy())) {
            rows = statsMapper.grouped(query);
            for (StatsRowVO row : rows) {
                fillRates(row);
            }
        }

        StatsResultVO result = new StatsResultVO();
        result.setSummary(summary);
        result.setRows(rows);
        return result;
    }

    /** 今日预约执行卡片 */
    @Transactional(readOnly = true)
    public StatsSummaryVO today(LoginUser operator) {
        StatsQueryDTO query = new StatsQueryDTO();
        query.setStartDate(LocalDate.now());
        query.setEndDate(LocalDate.now());
        return stats(query, operator).getSummary();
    }

    /** 导出 Excel：有分组导分组明细，无分组导单行汇总；表头写明口径 */
    @Transactional(readOnly = true)
    public void export(StatsQueryDTO query, LoginUser operator, HttpServletResponse response) throws IOException {
        StatsResultVO data = stats(query, operator);

        String[] headers = {"分组", "预约总数", "取消数", "失约数", "放行数",
                "核验通过数(已排除重复核验)", "核验失败数", "准时核验数",
                "失约率% = NO_SHOW/(总数-取消)", "准点率% = 准时/核验通过", "放行率% = RELEASED/(总数-取消)"};

        List<StatsRowVO> rows = data.getRows();
        if (rows == null || rows.isEmpty()) {
            StatsRowVO single = toRow("汇总", data.getSummary());
            rows = List.of(single);
        }
        ExcelExporter.export("预约执行统计", headers, rows, row -> new Object[]{
                row.getGroupKey(),
                row.getTotal(), row.getCancelled(), row.getNoShow(), row.getReleased(),
                row.getPassCount(), row.getFailCount(), row.getOnTimeCount(),
                row.getNoShowRate(), row.getPunctualRate(), row.getReleaseRate()
        }, "预约执行统计_" + query.getStartDate() + "_" + query.getEndDate(), response);
    }

    private StatsRowVO toRow(String key, StatsSummaryVO s) {
        StatsRowVO r = new StatsRowVO();
        r.setGroupKey(key);
        r.setTotal(s.getTotal());
        r.setCancelled(s.getCancelled());
        r.setNoShow(s.getNoShow());
        r.setReleased(s.getReleased());
        r.setPassCount(s.getPassCount());
        r.setFailCount(s.getFailCount());
        r.setOnTimeCount(s.getOnTimeCount());
        r.setNoShowRate(s.getNoShowRate());
        r.setPunctualRate(s.getPunctualRate());
        r.setReleaseRate(s.getReleaseRate());
        return r;
    }

    /** 入参归一：日期缺省今天、区间反了自动交换、分组合法、COMPANY 强制本公司 */
    private void normalize(StatsQueryDTO q, LoginUser operator) {
        LocalDate today = LocalDate.now();
        if (q.getStartDate() == null) {
            q.setStartDate(today);
        }
        if (q.getEndDate() == null) {
            q.setEndDate(today);
        }
        if (q.getStartDate().isAfter(q.getEndDate())) {
            LocalDate tmp = q.getStartDate();
            q.setStartDate(q.getEndDate());
            q.setEndDate(tmp);
        }
        if (StringUtils.hasText(q.getGroupBy())) {
            String groupBy = q.getGroupBy().trim().toUpperCase();
            if (!GROUP_BY.contains(groupBy)) {
                throw new BusinessException("不支持的分组维度：" + q.getGroupBy() + "（可选 LANE / SLOT / STATUS / COMPANY）");
            }
            q.setGroupBy(groupBy);
        } else {
            q.setGroupBy(null);
        }
        if (operator != null && Constants.ROLE_COMPANY.equals(operator.getRole())) {
            q.setCompanyId(operator.getCompanyId());
        }
    }

    private void fillRates(StatsSummaryVO s) {
        long base = nz(s.getTotal()) - nz(s.getCancelled());
        s.setNoShowRate(pct(nz(s.getNoShow()), base));
        s.setReleaseRate(pct(nz(s.getReleased()), base));
        s.setPunctualRate(pct(nz(s.getOnTimeCount()), nz(s.getPassCount())));
    }

    private void fillRates(StatsRowVO r) {
        long base = nz(r.getTotal()) - nz(r.getCancelled());
        r.setNoShowRate(pct(nz(r.getNoShow()), base));
        r.setReleaseRate(pct(nz(r.getReleased()), base));
        r.setPunctualRate(pct(nz(r.getOnTimeCount()), nz(r.getPassCount())));
    }

    /** 百分比，保留两位小数；分母 <= 0 返回 0，避免除零 */
    private double pct(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round(numerator * 10000.0 / denominator) / 100.0;
    }

    private long nz(Long value) {
        return value == null ? 0L : value;
    }
}
