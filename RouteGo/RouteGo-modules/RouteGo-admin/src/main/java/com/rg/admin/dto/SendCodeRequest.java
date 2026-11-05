package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送验证码入参。
 */
@Schema(description = "发送验证码入参")
@Data
public class SendCodeRequest {

    @Schema(description = "接收方：手机号或邮箱", example = "13800001111")
    @NotBlank(message = "请输入手机号或邮箱")
    private String target;

    @Schema(description = "场景：LOGIN 登录 / BIND 绑定 / RESET_PASSWORD 重置密码", example = "LOGIN")
    private String scene;
}
