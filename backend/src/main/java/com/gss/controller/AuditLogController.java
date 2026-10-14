package com.gss.controller;

import com.gss.common.PageResult;
import com.gss.common.Result;
import com.gss.entity.AuditLogEntity;
import com.gss.mapper.AuditLogMapper;
import com.gss.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 操作审计日志查询接口（仅管理员）。
 * 纯只读查询，直接注入 Mapper，不单独建 Service。
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "操作审计日志")
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;

    /**
     * 分页查询操作日志，支持按操作人、操作内容、结果筛选
     */
    @GetMapping
    @RequireRole("ADMIN")
    @Operation(summary = "分页查询操作日志")
    public Result<PageResult<AuditLogEntity>> page(@RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer size,
                                                   @RequestParam(required = false) String username,
                                                   @RequestParam(required = false) String operation,
                                                   @RequestParam(required = false) Integer success,
                                                   @RequestParam(required = false) LocalDate startTime,
                                                   @RequestParam(required = false) LocalDate endTime) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;
        List<AuditLogEntity> rows = auditLogMapper.selectPage(
                username, operation, success, startTime, endTime, (p - 1) * s, s);
        Long total = auditLogMapper.countPage(username, operation, success, startTime, endTime);
        return Result.success(PageResult.of(total, rows));
    }
}
