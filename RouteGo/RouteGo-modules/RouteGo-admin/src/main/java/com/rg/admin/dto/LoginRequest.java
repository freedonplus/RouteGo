package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 后台登录入参。
 * <p>
 * {@code account} 是统一账号输入框，支持用户名、手机号、邮箱三种形式；
 * {@code username} 仅为兼容旧前端保留，新前端请使用 {@code account}。
 * {@code captchaUuid} / {@code captchaCode} 来自 {@code GET /admin/auth/captcha}。
 */
@Schema(description = "后台登录入参")
@Data
public class LoginRequest {

    @Schema(description = "登录账号，支持用户名 / 手机号 / 邮箱", example = "your_admin")
    private String account;

    @Schema(description = "登录账号（旧字段，兼容保留）", deprecated = true)
    private String username;

    @Schema(description = "登录密码（明文，服务端按 BCrypt 校验）")
    private String password;

    @Schema(description = "验证码标识")
    private String captchaUuid;

    @Schema(description = "验证码字符（不区分大小写）")
    private String captchaCode;

    /**
     * 统一账号取值：优先 {@code account}，为空时回退旧字段 {@code username}。
     */
    public String resolveAccount() {
        String value = StringUtils.hasText(account) ? account : username;
        return value == null ? null : value.trim();
    }

    @AssertTrue(message = "请输入账号")
    @Schema(hidden = true)
    public boolean isAccountPresent() {
        return StringUtils.hasText(resolveAccount());
    }

    @AssertTrue(message = "请输入密码")
    @Schema(hidden = true)
    public boolean isPasswordPresent() {
        return StringUtils.hasText(password);
    }

    @AssertTrue(message = "请获取验证码")
    @Schema(hidden = true)
    public boolean isCaptchaPresent() {
        return StringUtils.hasText(captchaUuid) && StringUtils.hasText(captchaCode);
    }
}
