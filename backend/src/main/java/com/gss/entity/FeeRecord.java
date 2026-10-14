package com.gss.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收费记录实体（fee_record）
 */
@Data
public class FeeRecord {

    /** 收费记录ID */
    private Long id;

    /** 关联单证ID */
    private Long eirId;

    /** 车牌号 */
    private String plateNo;

    /** 缴费公司ID */
    private Long companyId;

    /** 应收金额 */
    private BigDecimal amount;

    /** 缴费状态：0未缴 1已缴 */
    private Integer payStatus;

    /** 缴费时间 */
    private LocalDateTime payTime;

    /** 操作人 */
    private String operator;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
