package com.gss.service;

import com.gss.common.Constants;
import com.gss.mapper.AppointmentMapper;
import com.gss.mapper.DriverMapper;
import com.gss.mapper.EirOrderMapper;
import com.gss.mapper.FeeRecordMapper;
import com.gss.mapper.TractorMapper;
import com.gss.mapper.TransportCompanyMapper;
import com.gss.security.LoginUser;
import com.gss.security.UserContext;
import com.gss.vo.FeeReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页看板服务：按角色返回统计数据（数据按 JWT 身份自动过滤）
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EirOrderMapper eirMapper;
    private final AppointmentMapper appointmentMapper;
    private final TractorMapper tractorMapper;
    private final DriverMapper driverMapper;
    private final TransportCompanyMapper companyMapper;
    private final FeeRecordMapper feeMapper;

    /**
     * 获取看板数据
     */
    public Map<String, Object> dashboard() {
        LoginUser user = UserContext.require();
        if (Constants.ROLE_COMPANY.equals(user.getRole())) {
            return buildCompanyDashboard(user.getCompanyId());
        }
        if (Constants.ROLE_DRIVER.equals(user.getRole())) {
            return buildDriverDashboard(user.getDriverId());
        }
        // ADMIN / GATE 看全局数据
        return buildGlobalDashboard();
    }

    /**
     * 可视化大屏数据
     */
    public Map<String, Object> screen() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 今日进出闸量
        data.put("todayGateIn", eirMapper.countByStatusOnDate("GATE_IN", null));
        data.put("todayGateOut", eirMapper.countByStatusOnDate("GATE_OUT", null));

        // 收费统计
        data.put("paidAmount", feeMapper.sumPaidAmount());
        data.put("unpaidAmount", feeMapper.sumUnpaidAmount());

        // 单证状态分布（前端按 key 直接取值）
        Map<String, Long> statusCount = new LinkedHashMap<>();
        statusCount.put("CREATED", eirMapper.countByStatus(Constants.ST_CREATED, null));
        statusCount.put("DISPATCHED", eirMapper.countByStatus(Constants.ST_DISPATCHED, null));
        statusCount.put("APPOINTED", eirMapper.countByStatus(Constants.ST_APPOINTED, null));
        statusCount.put("GATE_IN", eirMapper.countByStatus(Constants.ST_GATE_IN, null));
        statusCount.put("GATE_OUT", eirMapper.countByStatus(Constants.ST_GATE_OUT, null));
        statusCount.put("DONE", eirMapper.countByStatus(Constants.ST_DONE, null));
        statusCount.put("CANCELLED", eirMapper.countByStatus(Constants.ST_CANCELLED, null));
        data.put("statusCount", statusCount);

        // 业务类型分布（转为 Map 方便前端取值）
        Map<String, Object> bizTypeCount = new LinkedHashMap<>();
        for (Map<String, Object> row : eirMapper.countByBusinessType()) {
            bizTypeCount.put((String) row.get("bizType"), row.get("count"));
        }
        data.put("bizTypeCount", bizTypeCount);

        // 最近7天进出闸趋势
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        List<Map<String, Object>> dailyTrend = eirMapper.countDailyGateFlow(start.toString(), end.toString());
        data.put("dailyTrend", dailyTrend);

        return data;
    }

    /**
     * 码头端全局看板
     */
    private Map<String, Object> buildGlobalDashboard() {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("totalEir", eirMapper.countAll(null));
        data.put("createdCount", eirMapper.countByStatus(Constants.ST_CREATED, null));
        data.put("dispatchedCount", eirMapper.countByStatus(Constants.ST_DISPATCHED, null));
        data.put("appointedCount", eirMapper.countByStatus(Constants.ST_APPOINTED, null));
        data.put("gateInCount", eirMapper.countByStatus(Constants.ST_GATE_IN, null));
        data.put("gateOutCount", eirMapper.countByStatus(Constants.ST_GATE_OUT, null));
        data.put("doneCount", eirMapper.countByStatus(Constants.ST_DONE, null));
        data.put("cancelledCount", eirMapper.countByStatus(Constants.ST_CANCELLED, null));

        data.put("activeApptCount", appointmentMapper.countPage(null, null, Constants.APPT_ACTIVE, null, null));
        data.put("companyCount", companyMapper.countPage(null, null));
        data.put("tractorCount", tractorMapper.countPage(null, null, null));
        data.put("driverCount", driverMapper.countPage(null, null, null));
        data.put("paidAmount", feeMapper.sumPaidAmount());

        return data;
    }

    /**
     * 运输公司看板（仅本公司数据）
     */
    private Map<String, Object> buildCompanyDashboard(Long companyId) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("totalEir", eirMapper.countAll(companyId));
        data.put("createdCount", eirMapper.countByStatus(Constants.ST_CREATED, companyId));
        data.put("dispatchedCount", eirMapper.countByStatus(Constants.ST_DISPATCHED, companyId));
        data.put("appointedCount", eirMapper.countByStatus(Constants.ST_APPOINTED, companyId));
        data.put("gateInCount", eirMapper.countByStatus(Constants.ST_GATE_IN, companyId));
        data.put("gateOutCount", eirMapper.countByStatus(Constants.ST_GATE_OUT, companyId));
        data.put("doneCount", eirMapper.countByStatus(Constants.ST_DONE, companyId));
        data.put("cancelledCount", eirMapper.countByStatus(Constants.ST_CANCELLED, companyId));

        data.put("activeApptCount", appointmentMapper.countPage(null, null, Constants.APPT_ACTIVE, companyId, null));
        data.put("tractorCount", tractorMapper.countPage(null, null, companyId));
        data.put("driverCount", driverMapper.countPage(null, null, companyId));

        // 本公司缴费汇总
        List<FeeReportVO> reports = feeMapper.selectReport(companyId);
        BigDecimal paidAmount = reports.stream()
                .map(FeeReportVO::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("paidAmount", paidAmount);

        return data;
    }

    /**
     * 司机看板（仅本人预约）
     */
    private Map<String, Object> buildDriverDashboard(Long driverId) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("totalAppt", appointmentMapper.countPage(null, driverId, null, null, null));
        data.put("activeApptCount", appointmentMapper.countPage(null, driverId, Constants.APPT_ACTIVE, null, null));
        data.put("cancelledApptCount", appointmentMapper.countPage(null, driverId, Constants.APPT_CANCELLED, null, null));

        return data;
    }
}
