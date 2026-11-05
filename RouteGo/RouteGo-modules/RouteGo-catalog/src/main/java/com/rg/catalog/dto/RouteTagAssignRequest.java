package com.rg.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 线路标签绑定入参：整体覆盖该线路的标签。
 */
@Schema(description = "线路标签绑定入参，整体覆盖")
@Data
public class RouteTagAssignRequest {

    @Schema(description = "标签 ID 列表；传空数组表示清空该线路的标签")
    @NotNull(message = "标签列表不能为空")
    private List<Long> tagIds = new ArrayList<>();
}
