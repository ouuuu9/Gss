package com.gss.controller;

import com.gss.annotation.AuditLog;
import com.gss.annotation.RefreshScreen;
import com.gss.common.Result;
import com.gss.dto.LaneCapacityBatchDTO;
import com.gss.dto.LaneDTO;
import com.gss.security.RequireRole;
import com.gss.service.LaneService;
import com.gss.vo.LaneCapacityCellVO;
import com.gss.vo.LaneVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 闸口车道与配额接口（M3）
 */
@RestController
@RequestMapping("/api/lanes")
@RequiredArgsConstructor
@Tag(name = "闸口车道与配额")
public class LaneController {

    private final LaneService laneService;

    @GetMapping
    @RequireRole({"GATE", "ADMIN"})
    @Operation(summary = "车道列表")
    public Result<List<LaneVO>> list() {
        return Result.success(laneService.list());
    }

    @PostMapping
    @RequireRole("ADMIN")
    @AuditLog("新增车道")
    @RefreshScreen
    @Operation(summary = "新增车道")
    public Result<Void> create(@Valid @RequestBody LaneDTO dto) {
        laneService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    @AuditLog("修改车道")
    @RefreshScreen
    @Operation(summary = "修改车道")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody LaneDTO dto) {
        laneService.update(id, dto);
        return Result.success();
    }

    /** toggle?enabled=true/false；停用存在未完成预约的车道会被拒绝 */
    @PostMapping("/{id}/toggle")
    @RequireRole("ADMIN")
    @AuditLog("启停车道")
    @RefreshScreen
    @Operation(summary = "启用/停用车道")
    public Result<Void> toggle(@PathVariable Long id, @RequestParam boolean enabled) {
        laneService.toggle(id, enabled);
        return Result.success();
    }

    @GetMapping("/capacity")
    @RequireRole({"GATE", "DRIVER", "COMPANY", "ADMIN"})
    @Operation(summary = "车道×时段容量矩阵")
    public Result<List<LaneCapacityCellVO>> capacity(@RequestParam LocalDate date) {
        return Result.success(laneService.capacity(date));
    }

    @PostMapping("/capacity/batch")
    @RequireRole("ADMIN")
    @AuditLog("批量配置车道容量")
    @RefreshScreen
    @Operation(summary = "按日期批量配置容量")
    public Result<Void> batchCapacity(@Valid @RequestBody LaneCapacityBatchDTO dto) {
        laneService.batchCapacity(dto);
        return Result.success();
    }
}
