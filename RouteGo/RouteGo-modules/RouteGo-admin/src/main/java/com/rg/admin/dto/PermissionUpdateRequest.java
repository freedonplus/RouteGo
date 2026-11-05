package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 编辑权限点入参；权限码可选修改，修改时校验唯一。
 */
@Schema(description = "编辑权限点入参")
@Data
public class PermissionUpdateRequest {

    @Schema(description = "权限名称", example = "查看用户")
    private String permissionName;

    @Schema(description = "权限码，留空表示不修改", example = "system:user:list")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]*(?::[A-Za-z0-9_-]+)+$",
            message = "权限码格式应为 模块:动作（如 system:user:list）")
    private String permissionCode;

    @Schema(description = "所属模块", example = "system")
    private String module;

    @Schema(description = "说明")
    private String description;
}
