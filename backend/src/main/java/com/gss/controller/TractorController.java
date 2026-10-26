package com.gss.controller;

import com.gss.common.CompanyScope;
import com.gss.common.PageResult;
import com.gss.common.Result;
import com.gss.dto.TractorDTO;
import com.gss.entity.Tractor;
import com.gss.annotation.AuditLog;
import com.gss.security.RequireRole;
import com.gss.service.TractorService;
import com.gss.vo.TractorVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 拖车管理接口。
 * 数据权限：COMPANY 只能查询/维护本公司拖车（companyId 后端强制覆盖，越权 ID 由 Service 拒绝）
 */
@RestController
@RequestMapping("/api/tractors")
@RequiredArgsConstructor
@Tag(name = "拖车管理")
public class TractorController {

    private final TractorService tractorService;

    @GetMapping
    @Operation(summary = "分页查询拖车")
    public Result<PageResult<TractorVO>> page(@RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size,
                                              @RequestParam(required = false) String plateNo,
                                              @RequestParam(required = false) String icCardNo,
                                              @RequestParam(required = false) Long companyId) {
        return Result.success(tractorService.page(page, size, plateNo, icCardNo, CompanyScope.resolve(companyId)));
    }

    @GetMapping("/{id}")
    @RequireRole({"ADMIN", "GATE", "COMPANY"})
    @Operation(summary = "查询拖车详情")
    public Result<Tractor> detail(@PathVariable Long id) {
        return Result.success(tractorService.getById(id));
    }

    @PostMapping
    @RequireRole({"ADMIN", "COMPANY"})
    @AuditLog("新增拖车")
    @Operation(summary = "新增拖车")
    public Result<Void> create(@Valid @RequestBody TractorDTO dto) {
        dto.setCompanyId(CompanyScope.resolve(dto.getCompanyId()));
        tractorService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "COMPANY"})
    @AuditLog("修改拖车")
    @Operation(summary = "修改拖车")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TractorDTO dto) {
        dto.setCompanyId(CompanyScope.resolve(dto.getCompanyId()));
        tractorService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "COMPANY"})
    @AuditLog("删除拖车")
    @Operation(summary = "删除拖车")
    public Result<Void> delete(@PathVariable Long id) {
        tractorService.delete(id);
        return Result.success();
    }
}
