package com.gss.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 收费报表 VO：按运输公司汇总缴费笔数与金额
 */
@Data
public class FeeReportVO {

    private Long companyId;
    /** 公司编码 */
    private String companyCode;
    /** 公司名称 */
    private String companyName;
    /** 收费总笔数 */
    private Integer totalCount;
    /** 已缴费笔数 */
    private Integer paidCount;
    /** 未缴费笔数 */
    private Integer unpaidCount;
    /** 已缴费总金额 */
    private BigDecimal paidAmount;
    /** 应收总金额 */
    private BigDecimal totalAmount;
}
