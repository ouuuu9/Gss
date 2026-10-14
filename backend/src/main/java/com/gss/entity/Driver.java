package com.gss.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 司机实体（driver）
 */
@Data
public class Driver {

    /** 司机ID */
    private Long id;

    /** 司机姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 驾驶证号 */
    private String licenseNo;

    /** 绑定拖车ID */
    private Long tractorId;

    /** 所属公司ID */
    private Long companyId;

    /** 状态：1正常 0停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
