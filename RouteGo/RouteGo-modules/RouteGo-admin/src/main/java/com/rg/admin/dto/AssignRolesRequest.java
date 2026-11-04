package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 用户分配角色入参（全量替换）。
 */
@Schema(description = "用户分配角色入参")
@Data
public class AssignRolesRequest {

    @Schema(description = "角色 ID 列表，至少一个", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "至少分配一个角色")
    private List<Long> roleIds;
}
