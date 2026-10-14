package com.gss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增/修改拖车请求
 */
@Data
public class TractorDTO {

    @NotBlank(message = "车牌号不能为空")
    private String plateNo;

    private String frameNo;

    @NotBlank(message = "IC卡编号不能为空")
    private String icCardNo;

    @NotNull(message = "所属公司不能为空")
    private Long companyId;

    private Integer axleNum;

    private Integer status;
}
