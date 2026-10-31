package com.gss.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：生成与解析 Token（HS256）
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT
     */
    public String generateToken(LoginUser user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("username", user.getUsername())
                .claim("name", user.getName())
                .claim("role", user.getRole())
                .claim("companyId", user.getCompanyId())
                .claim("driverId", user.getDriverId())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 解析 JWT，失败（过期/伪造）抛出异常
     */
    public LoginUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // 公司/司机维度只有部分角色有值，缺失时保持 null
        Long companyId = null;
        Object companyIdClaim = claims.get("companyId");
        if (companyIdClaim != null) {
            companyId = Long.valueOf(companyIdClaim.toString());
        }

        Long driverId = null;
        Object driverIdClaim = claims.get("driverId");
        if (driverIdClaim != null) {
            driverId = Long.valueOf(driverIdClaim.toString());
        }

        return LoginUser.builder()
                .userId(Long.valueOf(claims.getSubject()))
                .username(claims.get("username", String.class))
                .name(claims.get("name", String.class))
                .role(claims.get("role", String.class))
                .companyId(companyId)
                .driverId(driverId)
                .build();
    }
}
