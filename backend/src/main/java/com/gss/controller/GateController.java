package com.gss.controller;

import com.gss.annotation.AuditLog;
import com.gss.annotation.Idempotent;
import com.gss.annotation.RefreshScreen;
import com.gss.common.PageResult;
import com.gss.common.Result;
import com.gss.dto.AuditDTO;
import com.gss.dto.GateVerifyDTO;
import com.gss.dto.IcCardDTO;
import com.gss.entity.EirOrder;
import com.gss.security.RequireRole;
import com.gss.security.UserContext;
import com.gss.service.GateService;
import com.gss.vo.EirOrderVO;
import com.gss.vo.GateCheckLogVO;
import com.gss.vo.VerifyResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 闸口办单接口
 */
@RestController
@RequestMapping("/api/gate")
@RequiredArgsConstructor
@Tag(name = "闸口办单")
public class GateController {

    private final GateService gateService;

    /**
     * 进闸办单（GATE），内部执行预约与缴费拦截
     */
    @PostMapping("/checkin")
    @RequireRole({"GATE", "ADMIN"})
    @AuditLog("进闸办单")
    @RefreshScreen
    @Idempotent
    @Operation(summary = "进闸办单")
    public Result<EirOrder> checkin(@Valid @RequestBody IcCardDTO dto) {
        return Result.success("进闸办单成功", gateService.checkin(dto.getIcCardNo()));
    }

    /**
     * 出闸办单（GATE）
     */
    @PostMapping("/checkout")
    @RequireRole({"GATE", "ADMIN"})
    @AuditLog("出闸办单")
    @RefreshScreen
    @Idempotent
    @Operation(summary = "出闸办单")
    public Result<EirOrder> checkout(@Valid @RequestBody IcCardDTO dto) {
        return Result.success("出闸办单成功", gateService.checkout(dto.getIcCardNo()));
    }

    /**
     * 闸口凭证核验（GATE/ADMIN）：凭证号或 IC 卡至少给一个。
     * 核验失败也返回 200 + result=FAIL（带异常类型），方便闸口页直接展示原因。
     */
    @PostMapping("/verify")
    @RequireRole({"GATE", "ADMIN"})
    @AuditLog("闸口凭证核验")
    @RefreshScreen
    @Operation(summary = "闸口凭证核验")
    public Result<VerifyResultVO> verify(@RequestBody GateVerifyDTO dto) {
        return Result.success(gateService.verify(dto, UserContext.require()));
    }

    /**
     * 核验留痕分页（GATE/ADMIN）
     */
    @GetMapping("/check-logs")
    @RequireRole({"GATE", "ADMIN"})
    @Operation(summary = "核验留痕列表")
    public Result<PageResult<GateCheckLogVO>> checkLogs(@RequestParam(required = false) Integer page,
                                                        @RequestParam(required = false) Integer size,
                                                        @RequestParam(required = false) String result) {
        return Result.success(gateService.checkLogs(page, size, result));
    }

    /**
     * 审单（ADMIN）
     */
    @PostMapping("/audit")
    @RequireRole("ADMIN")
    @AuditLog("审单")
    @RefreshScreen
    @Operation(summary = "审单")
    public Result<EirOrder> audit(@Valid @RequestBody AuditDTO dto) {
        return Result.success("审单完成", gateService.audit(dto.getEirId()));
    }

    /**
     * 最近办单结果（近 5 条闸口动作台账）
     */
    @GetMapping("/recent-ops")
    @RequireRole({"GATE", "ADMIN"})
    @Operation(summary = "最近办单结果")
    public Result<List<EirOrderVO>> recentOps() {
        return Result.success(gateService.recentOps());
    }
}
