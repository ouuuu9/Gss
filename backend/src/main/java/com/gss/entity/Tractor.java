package com.gss.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拖车实体（tractor）
 */
@Data
public class Tractor {

    /** 拖车ID */
    private Long id;

    /** 拖头车牌号 */
    private String plateNo;

    /** 车架号 */
    private String frameNo;

    /** IC卡编号（模拟刷卡） */
    private String icCardNo;

    /** 所属公司ID */
    private Long companyId;

    /** 轴数 */
    private Integer axleNum;

    /** 状态：1正常 0停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
