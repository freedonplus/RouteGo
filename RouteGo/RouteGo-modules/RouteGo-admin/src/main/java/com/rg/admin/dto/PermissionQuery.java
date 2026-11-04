package com.rg.admin.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限点分页查询条件。
 */
@Schema(description = "权限点分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionQuery extends PageQuery {

    @Schema(description = "权限名称，模糊匹配")
    private String permissionName;

    @Schema(description = "权限码，模糊匹配")
    private String permissionCode;

    @Schema(description = "所属模块")
    private String module;

    @Schema(description = "状态：0 禁用，1 启用")
    private Byte status;
}
