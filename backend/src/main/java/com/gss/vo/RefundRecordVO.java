package com.gss.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录列表 VO（含单证编号、公司名）
 */
@Data
public class RefundRecordVO {

    private Long id;
    private String refundNo;
    private Long feeRecordId;
    private Long eirId;
    private String eirNo;
    private Long companyId;
    private String companyName;
    private BigDecimal amount;
    private String status;
    private String applicant;
    private LocalDateTime appliedAt;
    private String approver;
    private LocalDateTime approvedAt;
    private String rejectReason;
    private String remark;
    private LocalDateTime createTime;
}
