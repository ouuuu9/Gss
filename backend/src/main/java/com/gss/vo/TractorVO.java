package com.gss.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拖车列表 VO（含公司名称）
 */
@Data
public class TractorVO {

    private Long id;
    private String plateNo;
    private String frameNo;
    private String icCardNo;
    private Long companyId;
    /** 所属公司名称 */
    private String companyName;
    private Integer axleNum;
    private Integer status;
    private LocalDateTime createTime;
}
