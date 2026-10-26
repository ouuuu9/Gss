package com.gss.controller;

import com.gss.annotation.AuditLog;
import com.gss.annotation.Idempotent;
import com.gss.annotation.RefreshScreen;
import com.gss.common.CompanyScope;
import com.gss.common.ExcelExporter;
import com.gss.common.PageResult;
import com.gss.common.Result;
import com.gss.dto.BatchDispatchDTO;
import com.gss.dto.DispatchDTO;
import com.gss.dto.EirDTO;
import com.gss.entity.EirOrder;
import com.gss.security.RequireRole;
import com.gss.security.UserContext;
import com.gss.service.EirService;
import com.gss.vo.EirOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * EIR 单证接口
 */
@RestController
@RequestMapping("/api/eir")
@RequiredArgsConstructor
@Tag(name = "EIR单证管理")
public class EirController {

    private final EirService eirService;

    /**
     * 单证列表（COMPANY 自动限定本公司）
     */
    @GetMapping
    @Operation(summary = "分页查询单证")
    public Result<PageResult<EirOrderVO>> page(@RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size,
                                               @RequestParam(required = false) String eirNo,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String businessType,
                                               @RequestParam(required = false) Long companyId,
                                               @RequestParam(required = false) Long driverId) {
        var scope = CompanyScope.of(companyId, driverId);
        return Result.success(eirService.page(page, size, eirNo, status, businessType,
                scope.companyId(), scope.driverId()));
    }

    /**
     * 导出 EIR 单证 Excel（按角色过滤数据，与列表查询一致）
     */
    @GetMapping("/export")
    @RequireRole({"ADMIN", "GATE", "COMPANY", "DRIVER"})
    @Operation(summary = "导出EIR单证Excel")
    public void exportExcel(@RequestParam(required = false) String status,
                            @RequestParam(required = false) String bizType,
                            @RequestParam(required = false) Long companyId,
                            HttpServletResponse response) throws IOException {
        var scope = CompanyScope.of(companyId, null);
        // 复用分页查询，取一个足够大的 size 代替全量查询
        List<EirOrderVO> rows = eirService.page(1, 10000, null, status, bizType,
                scope.companyId(), scope.driverId()).getRows();

        String[] headers = {"单证编号", "业务类型", "箱号", "箱型", "公司名称", "拖车号",
                "司机姓名", "状态", "录入时间", "派单时间", "预约时间", "进闸时间",
                "出闸时间", "审单时间"};
        ExcelExporter.export("EIR单证", headers, rows, vo -> new Object[]{
                vo.getEirNo(),
                ExcelExporter.bizTypeLabel(vo.getBusinessType()),
                vo.getContainerNo(),
                vo.getContainerSize() == null ? null : vo.getContainerSize() + (vo.getContainerType() == null ? "" : vo.getContainerType()),
                vo.getCompanyName(),
                vo.getPlateNo(),
                vo.getDriverName(),
                ExcelExporter.eirStatusLabel(vo.getStatus()),
                vo.getCreateTime(),
                vo.getDispatchTime(),
                vo.getApptTime(),
                vo.getGateInTime(),
                vo.getGateOutTime(),
                vo.getAuditTime()
        }, "EIR单证_导出", response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询单证详情")
    public Result<EirOrder> detail(@PathVariable Long id) {
        return Result.success(eirService.getById(id, UserContext.require()));
    }

    /**
     * 录入单证（ADMIN）
     */
    @PostMapping
    @RequireRole("ADMIN")
    @AuditLog("录入单证")
    @RefreshScreen
    @Idempotent
    @Operation(summary = "录入单证")
    public Result<Void> create(@Valid @RequestBody EirDTO dto) {
        eirService.create(dto);
        return Result.success();
    }

    /**
     * 修改单证（ADMIN）
     */
    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    @AuditLog("修改单证")
    @RefreshScreen
    @Operation(summary = "修改单证")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody EirDTO dto) {
        eirService.update(id, dto);
        return Result.success();
    }

    /**
     * 作废单证（ADMIN / 本公司 COMPANY）
     */
    @PostMapping("/{id}/cancel")
    @RequireRole({"ADMIN", "COMPANY"})
    @AuditLog("作废单证")
    @RefreshScreen
    @Operation(summary = "作废单证")
    public Result<Void> cancel(@PathVariable Long id) {
        eirService.cancel(id, UserContext.require());
        return Result.success();
    }

    /**
     * 派单（COMPANY）
     */
    @PostMapping("/{id}/dispatch")
    @RequireRole("COMPANY")
    @AuditLog("单证派单")
    @RefreshScreen
    @Idempotent
    @Operation(summary = "派单")
    public Result<Void> dispatch(@PathVariable Long id, @Valid @RequestBody DispatchDTO dto) {
        eirService.dispatch(id, dto, UserContext.require());
        return Result.success();
    }

    /**
     * 批量派单（COMPANY）：多单证派给同一司机与拖车，
     * 逐条独立执行，返回成功/失败统计
     */
    @PostMapping("/batch-dispatch")
    @RequireRole("COMPANY")
    @AuditLog("批量派单")
    @RefreshScreen
    @Idempotent
    @Operation(summary = "批量派单")
    public Result<Map<String, Object>> batchDispatch(@Valid @RequestBody BatchDispatchDTO dto) {
        Map<String, Object> result = eirService.batchDispatch(dto, UserContext.require());
        return Result.success("批量派单完成：成功 " + result.get("success") + " 单，失败 " + result.get("failed") + " 单", result);
    }

    /**
     * 取消派单（COMPANY）
     */
    @PostMapping("/{id}/cancel-dispatch")
    @RequireRole("COMPANY")
    @AuditLog("取消派单")
    @RefreshScreen
    @Operation(summary = "取消派单")
    public Result<Void> cancelDispatch(@PathVariable Long id) {
        eirService.cancelDispatch(id, UserContext.require());
        return Result.success();
    }
}
