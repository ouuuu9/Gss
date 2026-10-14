package com.gss.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 闸口刷卡请求：IC 卡号（模拟刷卡）
 */
@Data
public class IcCardDTO {

    @NotBlank(message = "IC卡号不能为空")
    private String icCardNo;
}
