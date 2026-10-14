package com.gss.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收费记录列表 VO（含公司名、单证编号）
 */
@Data
public class FeeRecordVO {

    private Long id;
    private Long eirId;
    /** 单证编号 */
    private String eirNo;
    /** 业务类型 */
    private String businessType;
    private String plateNo;
    private Long companyId;
    /** 缴费公司名称 */
    private String companyName;
    private BigDecimal amount;
    private Integer payStatus;
    private LocalDateTime payTime;
    private String operator;
    private LocalDateTime createTime;
}
