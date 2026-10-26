package com.gss.controller;

import com.gss.annotation.AuditLog;
import com.gss.annotation.Idempotent;
import com.gss.annotation.RefreshScreen;
import com.gss.common.CompanyScope;
import com.gss.common.ExcelExporter;
import com.gss.common.PageResult;
import com.gss.common.Result;
import com.gss.dto.AppointmentDTO;
import com.gss.security.RequireRole;
import com.gss.security.UserContext;
import com.gss.service.AppointmentService;
import com.gss.vo.AppointmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 进港预约接口
 */
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "进港预约")
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * 预约列表。
     * DRIVER 强制本人；COMPANY 强制本公司；ADMIN/GATE 可全部查询。
     */
    @GetMapping
    @Operation(summary = "分页查询预约")
    public Result<PageResult<AppointmentVO>> page(@RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size,
                                                  @RequestParam(required = false) String eirNo,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String apptDate,
                                                  @RequestParam(required = false) Long driverId,
                                                  @RequestParam(required = false) Long companyId) {
        var scope = CompanyScope.of(companyId, driverId);
        return Result.success(appointmentService.page(page, size, eirNo, status, apptDate,
                scope.driverId(), scope.companyId()));
    }

    /**
     * 导出预约记录 Excel（按角色过滤数据，与列表查询一致）
     */
    @GetMapping("/export")
    @RequireRole({"ADMIN", "GATE", "COMPANY", "DRIVER"})
    @Operation(summary = "导出预约记录Excel")
    public void exportExcel(@RequestParam(required = false) String status,
                            HttpServletResponse response) throws IOException {
        var scope = CompanyScope.of(null, null);
        // 复用分页查询，取一个足够大的 size 代替全量查询
        List<AppointmentVO> rows = appointmentService.page(1, 10000, null, status, null,
                scope.driverId(), scope.companyId()).getRows();

        String[] headers = {"预约编号", "单证编号", "拖车号", "司机姓名", "预约日期",
                "预约时段", "状态", "预约时间"};
        ExcelExporter.export("预约记录", headers, rows, vo -> new Object[]{
                vo.getId(),
                vo.getEirNo(),
                vo.getPlateNo(),
                vo.getDriverName(),
                vo.getApptDate(),
                vo.getTimeSlot(),
                ExcelExporter.apptStatusLabel(vo.getStatus()),
                vo.getCreateTime()
        }, "预约记录_导出", response);
    }

    /**
     * 创建预约（COMPANY / DRIVER）
     */
    @PostMapping
    @RequireRole({"COMPANY", "DRIVER"})
    @AuditLog("创建进港预约")
    @RefreshScreen
    @Idempotent
    @Operation(summary = "创建预约")
    public Result<Void> create(@Valid @RequestBody AppointmentDTO dto) {
        appointmentService.create(dto, UserContext.require());
        return Result.success();
    }

    /**
     * 取消预约（COMPANY / DRIVER）
     */
    @PostMapping("/{id}/cancel")
    @RequireRole({"COMPANY", "DRIVER"})
    @AuditLog("取消进港预约")
    @RefreshScreen
    @Operation(summary = "取消预约")
    public Result<Void> cancel(@PathVariable Long id) {
        appointmentService.cancel(id, UserContext.require());
        return Result.success();
    }

    /**
     * 按单证取消预约（COMPANY / DRIVER）：单证页直接取消，无需先查预约 ID
     */
    @PostMapping("/cancel-by-eir/{eirId}")
    @RequireRole({"COMPANY", "DRIVER"})
    @AuditLog("取消进港预约")
    @RefreshScreen
    @Operation(summary = "按单证取消预约")
    public Result<Void> cancelByEir(@PathVariable Long eirId) {
        appointmentService.cancelByEir(eirId, UserContext.require());
        return Result.success();
    }

    /**
     * 查询某日期各时段剩余容量（创建预约弹窗中展示约满情况）
     */
    @GetMapping("/slots")
    @Operation(summary = "查询某日期各时段剩余容量")
    public Result<List<Map<String, Object>>> slots(@RequestParam String date) {
        return Result.success(appointmentService.slotCapacity(date));
    }
}
