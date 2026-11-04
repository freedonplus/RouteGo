package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 全量替换关联 ID 的入参（允许空集合表示清空）。
 */
@Schema(description = "全量替换关联 ID 的入参")
@Data
public class AssignIdsRequest {

    @Schema(description = "ID 列表；空集合表示清空授权", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "缺少 ID 列表")
    private List<Long> ids;
}
