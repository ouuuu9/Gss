package com.gss.dto;

import lombok.Data;

/**
 * 闸口核验请求（M2）：voucherNo 与 icCard 至少给一个。
 * laneCode 本轮只做留痕（lane_id 在 M3 与 gate_lane 一起引入）。
 */
@Data
public class GateVerifyDTO {

    /** 预约凭证号 */
    private String voucherNo;

    /** IC 卡号（与 voucherNo 二选一或同时给） */
    private String icCard;

    /** 车道号（本轮仅记录到核验备注） */
    private String laneCode;
}
