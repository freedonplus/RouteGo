package com.rg.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置会员登录密码。
 */
@Schema(description = "会员密码重置入参")
@Data
public class CustomerPasswordRequest {

    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需在 8~32 位之间")
    private String password;
}
