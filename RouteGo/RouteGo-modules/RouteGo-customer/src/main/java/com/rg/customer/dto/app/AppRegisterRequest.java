package com.rg.customer.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * C 端注册/补设密码入参：手机号验证码确认身份后落库。
 */
@Schema(description = "App 注册入参")
@Data
public class AppRegisterRequest {

    @Schema(description = "手机号", example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "短信验证码")
    @NotBlank(message = "验证码不能为空")
    private String code;

    @Schema(description = "密码，6-32 位")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String password;

    @Schema(description = "用户名（可选），2-20 位且唯一；留空自动生成「用户+手机尾号」。填了之后可用它做密码登录账号")
    @Size(min = 2, max = 20, message = "用户名需 2-20 个字符")
    private String nickname;
}
