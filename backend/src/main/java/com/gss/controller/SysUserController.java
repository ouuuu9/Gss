package com.gss.controller;

import com.gss.common.PageResult;
import com.gss.common.Result;
import com.gss.dto.PasswordDTO;
import com.gss.dto.UserDTO;
import com.gss.entity.SysUser;
import com.gss.annotation.AuditLog;
import com.gss.security.RequireRole;
import com.gss.service.SysUserService;
import com.gss.vo.SysUserVO;
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
 * 系统用户管理接口（仅 ADMIN）
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@RequireRole("ADMIN")
@Tag(name = "系统用户管理")
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping
    @Operation(summary = "分页查询系统用户")
    public Result<PageResult<SysUserVO>> page(@RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size,
                                              @RequestParam(required = false) String username,
                                              @RequestParam(required = false) String role) {
        return Result.success(sysUserService.page(page, size, username, role));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情")
    public Result<SysUser> detail(@PathVariable Long id) {
        return Result.success(sysUserService.getById(id));
    }

    @PostMapping
    @AuditLog("新增用户")
    @Operation(summary = "新增用户")
    public Result<Void> create(@Valid @RequestBody UserDTO dto) {
        sysUserService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @AuditLog("修改用户")
    @Operation(summary = "修改用户")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserDTO dto) {
        sysUserService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @AuditLog("删除用户")
    @Operation(summary = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/password")
    @AuditLog("重置用户密码")
    @Operation(summary = "重置用户密码")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordDTO dto) {
        sysUserService.resetPassword(id, dto.getPassword());
        return Result.success();
    }
}
