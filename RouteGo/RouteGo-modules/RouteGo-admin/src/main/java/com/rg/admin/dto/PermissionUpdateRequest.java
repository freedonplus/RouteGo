package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 编辑权限点入参；权限码不可修改。
 */
@Schema(description = "编辑权限点入参")
@Data
public class PermissionUpdateRequest {

    @Schema(description = "权限名称", example = "查看用户")
    private String permissionName;

    @Schema(description = "所属模块", example = "system")
    private String module;

    @Schema(description = "说明")
    private String description;
}
