package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 编辑系统用户入参；账号不可修改，手机号/邮箱提交掩码值时视为不修改。
 */
@Schema(description = "编辑系统用户入参")
@Data
public class UserUpdateRequest {

    @Schema(description = "乐观锁版本号，取自详情接口", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "缺少版本号")
    private Integer version;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "手机号；null、空串或带 * 的掩码值表示不修改", example = "13812348000")
    private String phone;

    @Schema(description = "邮箱；null、空串或带 * 的掩码值表示不修改", example = "ops@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "备注")
    private String remark;
}
