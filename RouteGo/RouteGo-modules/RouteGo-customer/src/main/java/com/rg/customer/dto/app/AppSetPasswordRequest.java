package com.rg.customer.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * C 端设置 / 修改登录密码入参（需登录态）。
 * <p>
 * 主要用于「验证码登录进来但账号还没有密码」的补设场景。
 */
@Schema(description = "App 设置登录密码入参")
@Data
public class AppSetPasswordRequest {

    @Schema(description = "新密码，6-32 位", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String password;
}
