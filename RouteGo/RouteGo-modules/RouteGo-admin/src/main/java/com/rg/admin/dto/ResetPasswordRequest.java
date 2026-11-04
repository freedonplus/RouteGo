package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员重置用户密码入参。
 */
@Schema(description = "管理员重置用户密码入参")
@Data
public class ResetPasswordRequest {

    @Schema(description = "新密码：8–20 位，至少包含字母和数字",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入新密码")
    private String newPassword;
}
