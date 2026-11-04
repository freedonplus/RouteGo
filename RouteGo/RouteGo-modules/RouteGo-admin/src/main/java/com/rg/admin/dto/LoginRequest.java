package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 后台登录入参。
 * <p>
 * {@code username} 会去除首尾空白后匹配 {@code sys_user.username}；
 * {@code captchaUuid} / {@code captchaCode} 来自 {@code GET /admin/auth/captcha}。
 */
@Schema(description = "后台登录入参")
@Data
public class LoginRequest {

    @Schema(description = "登录账号（去除首尾空白后匹配 sys_user.username）",
            example = "your_admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入账号")
    private String username;

    @Schema(description = "登录密码（明文，服务端按 BCrypt 校验）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入密码")
    private String password;

    @Schema(description = "验证码标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请获取验证码")
    private String captchaUuid;

    @Schema(description = "验证码字符（不区分大小写）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入验证码")
    private String captchaCode;
}
