package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 编辑菜单入参。
 */
@Schema(description = "编辑菜单入参")
@Data
public class MenuUpdateRequest {

    @Schema(description = "乐观锁版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "缺少版本号")
    private Integer version;

    @Schema(description = "父级 ID，顶级为 0", example = "0")
    private Long parentId = 0L;

    @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入菜单名称")
    private String menuName;

    @Schema(description = "类型：M 目录，C 菜单，F 按钮", example = "C",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请选择菜单类型")
    @Pattern(regexp = "^[MCF]$", message = "菜单类型只能是 M/C/F")
    private String menuType;

    @Schema(description = "前端路由地址")
    private String path;

    @Schema(description = "前端组件路径")
    private String component;

    @Schema(description = "权限标识", example = "system:user:list")
    private String perms;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "是否显示：0 隐藏，1 显示", example = "1")
    private Byte visible;

    @Schema(description = "状态：0 禁用，1 启用", example = "1")
    private Byte status;
}
