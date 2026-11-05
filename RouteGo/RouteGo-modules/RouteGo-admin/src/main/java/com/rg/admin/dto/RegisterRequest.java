package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 手机号验证码注册入参。
 * <p>
 * 注册成功后账号默认绑定「普通用户」角色，并直接签发登录态。
 */
@Data
@Schema(description = "手机号验证码注册入参")
public class RegisterRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    @Schema(description = "手机号，同时作为登录账号", example = "13812340000")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "短信验证码", example = "123456")
    private String code;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码需为 8–20 位")
    @Schema(description = "登录密码，8-20 位且至少包含字母和数字")
    private String password;

    @Size(max = 50, message = "昵称最长 50 字")
    @Schema(description = "昵称，留空则自动生成")
    private String nickname;
}
