package com.gss.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志实体（audit_log）。
 * 命名为 AuditLogEntity 以避免与 @AuditLog 注解同名。
 */
@Data
public class AuditLogEntity {

    /** 日志ID */
    private Long id;

    /** 操作人用户名（未认证请求可能为空，如登录失败） */
    private String username;

    /** 操作人角色 */
    private String role;

    /** 操作描述（注解 value） */
    private String operation;

    /** HTTP方法 */
    private String method;

    /** 请求URI */
    private String uri;

    /** 请求参数（JSON，敏感字段脱敏，截断至500字符） */
    private String params;

    /** 客户端IP */
    private String ip;

    /** 是否成功：1成功 0失败 */
    private Integer success;

    /** 失败原因 */
    private String errorMsg;

    /** 耗时（毫秒） */
    private Integer costMs;

    /** 操作时间 */
    private LocalDateTime createTime;
}
