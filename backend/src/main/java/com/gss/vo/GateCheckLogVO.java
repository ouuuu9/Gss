package com.gss.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 闸口核验记录 VO（含单证编号与操作人姓名）
 */
@Data
public class GateCheckLogVO {

    private Long id;
    private String voucherNo;
    private Long eirId;
    /** 单证编号 */
    private String eirNo;
    private Long appointmentId;
    private LocalDateTime checkTime;
    private String result;
    private String abnormalType;
    private Long operatorId;
    /** 操作人姓名 */
    private String operatorName;
    private String remark;
}
