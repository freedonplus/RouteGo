package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 后台登录入参。
 */
@Schema(description = "后台登录入参")
@Data
public class LoginRequest {

    @Schema(description = "登录账号", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入账号")
    private String username;

    @Schema(description = "登录密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入密码")
    private String password;
}
