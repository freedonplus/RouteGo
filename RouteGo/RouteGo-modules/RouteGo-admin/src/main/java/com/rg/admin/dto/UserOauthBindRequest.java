package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "绑定第三方账号入参")
@Data
public class UserOauthBindRequest {

    @Schema(description = "用户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "缺少用户 ID")
    private Long userId;

    @Schema(description = "平台：wechat、qq、apple、dingtalk", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请选择平台")
    private String provider;

    @Schema(description = "平台用户唯一标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "缺少 openid")
    private String openid;

    @Schema(description = "开放平台统一标识")
    private String unionid;

    @Schema(description = "平台昵称")
    private String nickname;

    @Schema(description = "平台头像")
    private String avatar;
}
