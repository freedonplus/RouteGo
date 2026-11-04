package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 新增权限点入参。
 */
@Schema(description = "新增权限点入参")
@Data
public class PermissionCreateRequest {

    @Schema(description = "权限名称", example = "查看用户", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入权限名称")
    private String permissionName;

    @Schema(description = "权限码，创建后不可修改", example = "system:user:list",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入权限码")
    @Pattern(regexp = "^[a-z][a-z0-9_-]*(?::[a-z0-9_-]+)+$", message = "权限码格式应为 模块:动作")
    private String permissionCode;

    @Schema(description = "所属模块", example = "system")
    private String module;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "状态：0 禁用，1 启用", example = "1")
    private Byte status;
}
