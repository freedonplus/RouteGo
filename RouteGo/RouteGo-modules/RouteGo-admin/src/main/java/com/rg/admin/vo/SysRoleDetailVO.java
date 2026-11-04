package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 角色详情，包含已分配菜单与权限点。
 */
@Schema(description = "角色详情")
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleDetailVO extends SysRoleVO {

    @Schema(description = "已分配菜单 ID")
    private List<Long> menuIds;

    @Schema(description = "已分配权限点 ID")
    private List<Long> permissionIds;
}
