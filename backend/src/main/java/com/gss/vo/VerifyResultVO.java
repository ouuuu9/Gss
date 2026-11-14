package com.gss.vo;

import lombok.Data;

/**
 * 闸口核验结果（M2）：PASS/FAIL + 异常类型 + 文案，供闸口页展示。
 */
@Data
public class VerifyResultVO {

    /** PASS / FAIL */
    private String result;

    /** 异常类型（PASS 时为空） */
    private String abnormalType;

    /** 给操作员看的说明 */
    private String message;

    private String voucherNo;
    private String eirNo;
    private Long appointmentId;
}
