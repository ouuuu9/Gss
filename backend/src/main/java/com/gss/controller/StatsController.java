package com.gss.controller;

import com.gss.common.Result;
import com.gss.dto.StatsQueryDTO;
import com.gss.security.RequireRole;
import com.gss.security.UserContext;
import com.gss.service.StatsService;
import com.gss.vo.StatsResultVO;
import com.gss.vo.StatsSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 预约执行统计接口（M4/R5）
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "预约执行统计")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/appointment")
    @RequireRole({"ADMIN", "GATE", "COMPANY"})
    @Operation(summary = "预约执行统计（汇总 + 可选分组）")
    public Result<StatsResultVO> appointment(StatsQueryDTO query) {
        return Result.success(statsService.stats(query, UserContext.require()));
    }

    @GetMapping("/appointment/export")
    @RequireRole({"ADMIN", "GATE", "COMPANY"})
    @Operation(summary = "导出预约执行统计 Excel")
    public void export(StatsQueryDTO query, HttpServletResponse response) throws IOException {
        statsService.export(query, UserContext.require(), response);
    }

    @GetMapping("/today")
    @RequireRole({"ADMIN", "GATE", "COMPANY"})
    @Operation(summary = "今日预约执行卡片")
    public Result<StatsSummaryVO> today() {
        return Result.success(statsService.today(UserContext.require()));
    }
}
