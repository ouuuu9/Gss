package com.gss.controller;

import com.gss.annotation.AuditLog;
import com.gss.common.Result;
import com.gss.dto.LoginDTO;
import com.gss.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录、登出、获取当前用户")
public class AuthController {

    private final AuthService authService;

    /**
     * 登录，返回 JWT
     */
    @PostMapping("/login")
    @AuditLog("用户登录")
    @Operation(summary = "登录并返回JWT")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    @AuditLog("用户登出")
    @Operation(summary = "登出")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public Result<Map<String, Object>> me() {
        return Result.success(authService.me());
    }
}
