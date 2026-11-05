package com.rg.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 行程批量保存入参：整体覆盖某条线路的行程，常用于行程编辑器一次性提交。
 */
@Schema(description = "行程批量保存入参，整体覆盖该线路的行程")
@Data
public class RouteItineraryBatchRequest {

    @Schema(description = "行程明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请至少填写一天的行程")
    @Valid
    private List<RouteItinerarySaveRequest> items;
}
