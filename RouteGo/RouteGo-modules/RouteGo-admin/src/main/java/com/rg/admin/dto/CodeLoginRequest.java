package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 验证码登录入参。
 */
@Schema(description = "验证码登录入参")
@Data
public class CodeLoginRequest {

    @Schema(description = "登录账号：手机号或邮箱", example = "13800001111")
    @NotBlank(message = "请输入手机号或邮箱")
    private String target;

    @Schema(description = "验证码", example = "123456")
    @NotBlank(message = "请输入验证码")
    private String code;
}
