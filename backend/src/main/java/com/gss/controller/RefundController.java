package com.gss.controller;

import com.gss.annotation.AuditLog;
import com.gss.annotation.RefreshScreen;
import com.gss.common.CompanyScope;
import com.gss.common.PageResult;
import com.gss.common.Result;
import com.gss.dto.RefundRejectDTO;
import com.gss.security.RequireRole;
import com.gss.security.UserContext;
import com.gss.service.RefundService;
import com.gss.vo.RefundRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 退款申请与审批接口
 */
@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
@Tag(name = "退款管理")
public class RefundController {

    private final RefundService refundService;

    /**
     * 退款记录列表（COMPANY 只能看本公司）
     */
    @GetMapping
    @RequireRole({"ADMIN", "COMPANY"})
    @Operation(summary = "分页查询退款记录")
    public Result<PageResult<RefundRecordVO>> page(@RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer size,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long companyId) {
        return Result.success(refundService.page(page, size, status, CompanyScope.resolve(companyId)));
    }

    /**
     * 审批通过（ADMIN）：退款 + 作废同一事务
     */
    @PostMapping("/{id}/approve")
    @RequireRole("ADMIN")
    @AuditLog("退款审批通过")
    @RefreshScreen
    @Operation(summary = "退款审批通过")
    public Result<String> approve(@PathVariable Long id) {
        String outcome = refundService.approve(id, UserContext.require());
        if ("REJECTED_GATE_IN".equals(outcome)) {
            return Result.success("单证已进闸，退款申请已自动驳回", outcome);
        }
        return Result.success("退款完成，单证已作废", outcome);
    }

    /**
     * 驳回退款申请（ADMIN）
     */
    @PostMapping("/{id}/reject")
    @RequireRole("ADMIN")
    @AuditLog("退款驳回")
    @RefreshScreen
    @Operation(summary = "驳回退款申请")
    public Result<Map<String, Object>> reject(@PathVariable Long id,
                                              @Valid @RequestBody RefundRejectDTO dto) {
        refundService.reject(id, dto.getReason(), UserContext.require());
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        return Result.success("已驳回退款申请", data);
    }
}
