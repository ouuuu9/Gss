package com.gss.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 重置密码请求
 */
@Data
public class PasswordDTO {

    @NotBlank(message = "新密码不能为空")
    private String password;
}
