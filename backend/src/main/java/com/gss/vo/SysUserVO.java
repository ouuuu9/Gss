package com.gss.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户列表 VO（含公司名称）
 */
@Data
public class SysUserVO {

    private Long id;
    private String username;
    private String name;
    private String role;
    private Long companyId;
    /** 所属公司名称 */
    private String companyName;
    private Long driverId;
    private Integer status;
    private LocalDateTime createTime;
}
