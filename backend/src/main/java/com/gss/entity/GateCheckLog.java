package com.gss.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 闸口核验留痕（gate_check_log，M2）。
 * 每次核验尝试都会落一条，PASS/FAIL 均留痕；同一凭证重复核验也照样记录。
 */
@Data
public class GateCheckLog {

    /** 主键 */
    private Long id;

    /** 凭证号（IC 卡核验时也回填对应预约的凭证号） */
    private String voucherNo;

    /** 关联单证ID（查不到预约时为空） */
    private Long eirId;

    /** 关联预约ID（查不到预约时为空） */
    private Long appointmentId;

    /** 核验时间 */
    private LocalDateTime checkTime;

    /** 结果：PASS / FAIL */
    private String result;

    /** 异常类型：NO_APPOINTMENT / CANCELLED / EXPIRED / DOC_MISMATCH / EARLY / LATE */
    private String abnormalType;

    /** 操作人用户ID */
    private Long operatorId;

    /** 备注（IC 卡、车道号、多预约提示等） */
    private String remark;
}
