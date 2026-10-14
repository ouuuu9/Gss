package com.gss.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 司机列表 VO（含公司名称、绑定车牌）
 */
@Data
public class DriverVO {

    private Long id;
    private String name;
    private String phone;
    private String licenseNo;
    private Long tractorId;
    /** 绑定车牌号 */
    private String plateNo;
    private Long companyId;
    /** 所属公司名称 */
    private String companyName;
    private Integer status;
    private LocalDateTime createTime;
}
