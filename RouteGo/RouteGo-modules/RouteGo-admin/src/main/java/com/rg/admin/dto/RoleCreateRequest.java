package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 新增角色入参。
 */
@Schema(description = "新增角色入参")
@Data
public class RoleCreateRequest {

    @Schema(description = "角色名称", example = "运营", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入角色名称")
    private String roleName;

    @Schema(description = "角色编码，小写字母开头，创建后不可修改", example = "ops",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入角色编码")
    @Pattern(regexp = "^[a-z][a-z0-9_]{1,49}$", message = "角色编码需为小写字母、数字或下划线")
    private String roleCode;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "状态：0 禁用，1 启用", example = "1")
    private Byte status;

    @Schema(description = "备注")
    private String remark;
}
