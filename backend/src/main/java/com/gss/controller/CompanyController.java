package com.gss.controller;

import com.gss.common.PageResult;
import com.gss.common.Result;
import com.gss.dto.CompanyDTO;
import com.gss.entity.TransportCompany;
import com.gss.annotation.AuditLog;
import com.gss.security.RequireRole;
import com.gss.service.CompanyService;
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
 * 运输公司管理接口。
 * 查询：所有登录角色可用（下拉选择）；增删改：仅 ADMIN。
 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Tag(name = "运输公司管理")
public class CompanyController {

    private final CompanyService companyService;

    /**
     * 分页查询
     */
    @GetMapping
    @Operation(summary = "分页查询运输公司")
    public Result<PageResult<TransportCompany>> page(@RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size,
                                                     @RequestParam(required = false) String name,
                                                     @RequestParam(required = false) String code) {
        return Result.success(companyService.page(page, size, name, code));
    }

    /**
     * 全部公司（不分页，下拉选择用）
     */
    @GetMapping("/all")
    @Operation(summary = "查询全部公司（下拉选择用）")
    public Result<List<TransportCompany>> all() {
        return Result.success(companyService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询公司详情")
    public Result<TransportCompany> detail(@PathVariable Long id) {
        return Result.success(companyService.getById(id));
    }

    @PostMapping
    @RequireRole("ADMIN")
    @AuditLog("新增运输公司")
    @Operation(summary = "新增运输公司")
    public Result<Void> create(@Valid @RequestBody CompanyDTO dto) {
        companyService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    @AuditLog("修改运输公司")
    @Operation(summary = "修改运输公司")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CompanyDTO dto) {
        companyService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    @AuditLog("删除运输公司")
    @Operation(summary = "删除运输公司")
    public Result<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return Result.success();
    }
}
