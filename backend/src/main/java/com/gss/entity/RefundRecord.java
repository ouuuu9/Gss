package com.gss.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款申请/退款记录实体（refund_record）。
 * 状态：PENDING 待审批 / APPROVED 已退款 / REJECTED 已驳回。
 */
@Data
public class RefundRecord {

    /** 退款记录ID */
    private Long id;

    /** 退款编号（审批通过时生成） */
    private String refundNo;

    /** 关联收费记录ID */
    private Long feeRecordId;

    /** 关联单证ID */
    private Long eirId;

    /** 缴费公司ID */
    private Long companyId;

    /** 退款金额（按单证整笔退，等于原收费金额） */
    private BigDecimal amount;

    /** 状态：PENDING 待审批 / APPROVED 已退款 / REJECTED 已驳回 */
    private String status;

    /** 申请人 */
    private String applicant;

    /** 申请时间 */
    private LocalDateTime appliedAt;

    /** 审批人 */
    private String approver;

    /** 审批时间（驳回时也为处理时间） */
    private LocalDateTime approvedAt;

    /** 驳回原因 */
    private String rejectReason;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
