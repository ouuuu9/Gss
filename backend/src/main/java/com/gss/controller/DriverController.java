package com.gss.controller;

import com.gss.common.CompanyScope;
import com.gss.common.PageResult;
import com.gss.common.Result;
import com.gss.dto.DriverDTO;
import com.gss.entity.Driver;
import com.gss.annotation.AuditLog;
import com.gss.security.RequireRole;
import com.gss.service.DriverService;
import com.gss.vo.DriverVO;
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

import java.util.List;

/**
 * 司机管理接口。
 * 数据权限：COMPANY 只能查询/维护本公司司机（companyId 后端强制覆盖，越权 ID 由 Service 拒绝）
 */
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Tag(name = "司机管理")
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    @Operation(summary = "分页查询司机")
    public Result<PageResult<DriverVO>> page(@RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size,
                                             @RequestParam(required = false) String name,
                                             @RequestParam(required = false) String phone,
                                             @RequestParam(required = false) Long companyId) {
        return Result.success(driverService.page(page, size, name, phone, CompanyScope.resolve(companyId)));
    }

    /**
     * 全部司机（派单下拉用；COMPANY 只返回本公司司机）
     */
    @GetMapping("/all")
    @RequireRole({"ADMIN", "GATE", "COMPANY"})
    @Operation(summary = "查询全部司机（派单下拉用）")
    public Result<List<DriverVO>> all() {
        return Result.success(driverService.listAll());
    }

    @GetMapping("/{id}")
    @RequireRole({"ADMIN", "GATE", "COMPANY"})
    @Operation(summary = "查询司机详情")
    public Result<Driver> detail(@PathVariable Long id) {
        return Result.success(driverService.getById(id));
    }

    @PostMapping
    @RequireRole({"ADMIN", "COMPANY"})
    @AuditLog("新增司机")
    @Operation(summary = "新增司机")
    public Result<Void> create(@Valid @RequestBody DriverDTO dto) {
        dto.setCompanyId(CompanyScope.resolve(dto.getCompanyId()));
        driverService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "COMPANY"})
    @AuditLog("修改司机")
    @Operation(summary = "修改司机")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DriverDTO dto) {
        dto.setCompanyId(CompanyScope.resolve(dto.getCompanyId()));
        driverService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "COMPANY"})
    @AuditLog("删除司机")
    @Operation(summary = "删除司机")
    public Result<Void> delete(@PathVariable Long id) {
        boolean removed = driverService.delete(id);
        return removed ? Result.success()
                : Result.success("该司机存在历史单证，已改为停用状态保留记录", null);
    }
}
