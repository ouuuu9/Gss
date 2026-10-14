package com.gss.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运输公司实体（transport_company）
 */
@Data
public class TransportCompany {

    /** 公司ID */
    private Long id;

    /** 公司编码 */
    private String code;

    /** 公司名称 */
    private String name;

    /** 联系人 */
    private String contact;

    /** 联系电话 */
    private String phone;

    /** 状态：1正常 0停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
