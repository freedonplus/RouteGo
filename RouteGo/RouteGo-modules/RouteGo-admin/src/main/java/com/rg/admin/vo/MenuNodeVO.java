package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点。
 */
@Schema(description = "菜单树节点")
@Data
public class MenuNodeVO {

    @Schema(description = "菜单 ID")
    private Long id;

    @Schema(description = "父级 ID")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "类型：M 目录，C 菜单，F 按钮")
    private String menuType;

    @Schema(description = "前端路由地址")
    private String path;

    @Schema(description = "前端组件路径")
    private String component;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "是否显示：0 隐藏，1 显示")
    private Byte visible;

    @Schema(description = "状态：0 禁用，1 启用")
    private Byte status;

    @Schema(description = "子菜单数量")
    private Long childCount;

    @Schema(description = "引用该菜单的角色数量")
    private Long roleCount;

    @Schema(description = "子节点")
    private List<MenuNodeVO> children = new ArrayList<>();
}
