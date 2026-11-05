package com.rg.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量覆盖保存出行人：先清空该订单现有出行人，再按本次入参重建。
 */
@Schema(description = "批量保存出行人")
@Data
public class OrderTravelerBatchRequest {

    @Schema(description = "出行人清单", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请至少填写一位出行人")
    @Valid
    private List<OrderTravelerSaveRequest> items;
}
