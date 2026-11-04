package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 编辑角色入参；角色编码不可修改。
 */
@Schema(description = "编辑角色入参")
@Data
public class RoleUpdateRequest {

    @Schema(description = "乐观锁版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "缺少版本号")
    private Integer version;

    @Schema(description = "角色名称", example = "运营")
    private String roleName;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;
}
