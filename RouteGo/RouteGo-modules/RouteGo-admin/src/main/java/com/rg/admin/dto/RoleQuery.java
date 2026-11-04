package com.rg.admin.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色分页查询条件。
 */
@Schema(description = "角色分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQuery extends PageQuery {

    @Schema(description = "角色名称，模糊匹配")
    private String roleName;

    @Schema(description = "角色编码，模糊匹配")
    private String roleCode;

    @Schema(description = "状态：0 禁用，1 启用")
    private Byte status;
}
