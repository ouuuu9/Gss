package com.gss.controller;

import com.gss.annotation.AuditLog;
import com.gss.annotation.Idempotent;
import com.gss.annotation.RefreshScreen;
import com.gss.common.CompanyScope;
import com.gss.common.ExcelExporter;
import com.gss.common.PageResult;
import com.gss.common.Result;
import com.gss.dto.BatchPayDTO;
import com.gss.security.RequireRole;
import com.gss.service.FeeService;
import com.gss.vo.FeeRecordVO;
import com.gss.vo.FeeReportVO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收费接口
 */
@RestController
@RequestMapping("/api/fees")
@RequiredArgsConstructor
@Tag(name = "收费管理")
public class FeeController {

    private final FeeService feeService;

    /**
     * 收费记录列表（COMPANY 自动限定本公司）
     */
    @GetMapping
    @RequireRole({"ADMIN", "GATE", "COMPANY"})
    @Operation(summary = "分页查询收费记录")
    public Result<PageResult<FeeRecordVO>> page(@RequestParam(required = false) Integer page,
                                                @RequestParam(required = false) Integer size,
                                                @RequestParam(required = false) String eirNo,
                                                @RequestParam(required = false) Integer payStatus,
                                                @RequestParam(required = false) Long companyId) {
        return Result.success(feeService.page(page, size, eirNo, payStatus, CompanyScope.resolve(companyId)));
    }

    /**
     * 导出收费记录 Excel（按角色过滤数据，与列表查询一致）
     */
    @GetMapping("/export")
    @RequireRole({"ADMIN", "GATE", "COMPANY"})
    @Operation(summary = "导出收费记录Excel")
    public void exportExcel(@RequestParam(required = false) Integer payStatus,
                            @RequestParam(required = false) Long companyId,
                            HttpServletResponse response) throws IOException {
        // 复用分页查询，取一个足够大的 size 代替全量查询
        List<FeeRecordVO> rows = feeService.page(1, 10000, null, payStatus, CompanyScope.resolve(companyId)).getRows();

        String[] headers = {"收费编号", "单证编号", "公司名称", "业务类型", "金额",
                "缴费状态", "操作人", "缴费时间", "创建时间"};
        ExcelExporter.export("收费记录", headers, rows, vo -> new Object[]{
                vo.getId(),
                vo.getEirNo(),
                vo.getCompanyName(),
                ExcelExporter.bizTypeLabel(vo.getBusinessType()),
                vo.getAmount(),
                ExcelExporter.payStatusLabel(vo.getPayStatus()),
                vo.getOperator(),
                vo.getPayTime(),
                vo.getCreateTime()
        }, "收费记录_导出", response);
    }

    /**
     * 单笔缴费（COMPANY）
     */
    @PostMapping("/{id}/pay")
    @RequireRole("COMPANY")
    @AuditLog("缴纳费用")
    @RefreshScreen
    @Idempotent
    @Operation(summary = "单笔缴费")
    public Result<Void> pay(@PathVariable Long id) {
        feeService.pay(id);
        return Result.success("缴费成功", null);
    }

    /**
     * 批量缴费（COMPANY）
     */
    @PostMapping("/batch-pay")
    @RequireRole("COMPANY")
    @AuditLog("批量缴费")
    @RefreshScreen
    @Idempotent
    @Operation(summary = "批量缴费")
    public Result<Map<String, Object>> batchPay(@Valid @RequestBody BatchPayDTO dto) {
        int count = feeService.batchPay(dto.getIds());
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return Result.success("批量缴费成功，共缴费 " + count + " 笔", data);
    }

    /**
     * 收费报表（按公司汇总）。COMPANY 仅本公司。
     */
    @GetMapping("/report")
    @RequireRole({"ADMIN", "GATE", "COMPANY"})
    @Operation(summary = "收费报表（按公司汇总）")
    public Result<List<FeeReportVO>> report(@RequestParam(required = false) Long companyId) {
        return Result.success(feeService.report(CompanyScope.resolve(companyId)));
    }
}
