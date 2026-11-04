package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 按模块分组的权限点。
 */
@Schema(description = "按模块分组的权限点")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionGroupVO {

    @Schema(description = "模块名")
    private String module;

    @Schema(description = "模块下权限点")
    private List<SysPermissionVO> permissions;
}
