package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限点。
 */
@Schema(description = "权限点")
@Data
public class SysPermissionVO {

    @Schema(description = "权限点 ID")
    private Long id;

    @Schema(description = "权限名称")
    private String permissionName;

    @Schema(description = "权限码")
    private String permissionCode;

    @Schema(description = "所属模块")
    private String module;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "状态：0 禁用，1 启用")
    private Byte status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
