package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改本人密码入参。
 */
@Schema(description = "修改本人密码入参")
@Data
public class ChangePasswordRequest {

    @Schema(description = "旧密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入旧密码")
    private String oldPassword;

    @Schema(description = "新密码：8–20 位，至少包含字母和数字",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入新密码")
    private String newPassword;
}
