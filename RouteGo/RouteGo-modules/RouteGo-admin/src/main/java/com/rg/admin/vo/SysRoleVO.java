package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色列表项。
 */
@Schema(description = "角色列表项")
@Data
public class SysRoleVO {

    @Schema(description = "角色 ID", example = "1")
    private Long id;

    @Schema(description = "角色名称", example = "运营")
    private String roleName;

    @Schema(description = "角色编码", example = "ops")
    private String roleCode;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "状态：0 禁用，1 启用", example = "1")
    private Byte status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "乐观锁版本号")
    private Integer version;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
