package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.dto.LoginDTO;
import com.gss.entity.SysUser;
import com.gss.mapper.SysUserMapper;
import com.gss.security.JwtUtil;
import com.gss.security.LoginUser;
import com.gss.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务：登录、登出、获取当前用户
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 登录：校验用户名密码与账号状态，返回 JWT
     */
    public Map<String, Object> login(LoginDTO dto) {
        SysUser user = sysUserMapper.selectByUsername(dto.getUsername().trim());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        LoginUser loginUser = toLoginUser(user);
        String token = jwtUtil.generateToken(loginUser);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", buildUserInfo(user));
        return result;
    }

    /**
     * 登出：JWT 无状态，服务端无需处理，由前端丢弃 token
     */
    public void logout() {
        // 无状态 JWT：登出仅需前端清除本地 token
    }

    /**
     * 获取当前登录用户信息（密码等敏感字段不返回）
     */
    @Transactional(readOnly = true)
    public Map<String, Object> me() {
        LoginUser loginUser = UserContext.require();
        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return buildUserInfo(user);
    }

    /**
     * 组装返回前端的用户信息
     */
    private Map<String, Object> buildUserInfo(SysUser user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("name", user.getName());
        info.put("role", user.getRole());
        info.put("companyId", user.getCompanyId());
        info.put("driverId", user.getDriverId());
        info.put("status", user.getStatus());
        return info;
    }

    /**
     * 实体转登录用户（写入 JWT 的身份信息）
     */
    public static LoginUser toLoginUser(SysUser user) {
        return LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .role(user.getRole())
                .companyId(Constants.ROLE_ADMIN.equals(user.getRole())
                        || Constants.ROLE_GATE.equals(user.getRole()) ? null : user.getCompanyId())
                .driverId(user.getDriverId())
                .build();
    }
}
