package com.gss.controller;

import com.gss.common.Result;
import com.gss.security.RequireRole;
import com.gss.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 首页看板接口（仅 ADMIN / GATE 可见，公司与司机不可访问全码头运营数据）
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "首页看板", description = "看板统计数据")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @RequireRole({"ADMIN", "GATE"})
    @Operation(summary = "获取看板数据")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(dashboardService.dashboard());
    }

    @GetMapping("/screen")
    @RequireRole({"ADMIN", "GATE"})
    @Operation(summary = "可视化大屏数据")
    public Result<Map<String, Object>> screen() {
        return Result.success(dashboardService.screen());
    }
}
