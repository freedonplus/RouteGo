package com.rg.customer.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * C 端账号密码登录入参。
 * <p>
 * 「账号」一个输入框搞定：填手机号或用户名（昵称）都能登录，
 * 服务端按是否为 11 位手机号自动分流，前端不需要切换。
 */
@Schema(description = "App 账号密码登录入参")
@Data
public class AppAccountLoginRequest {

    @Schema(description = "账号：手机号或用户名", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入手机号或用户名")
    @Size(max = 32, message = "账号长度不能超过 32 位")
    private String account;

    @Schema(description = "登录密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String password;
}
