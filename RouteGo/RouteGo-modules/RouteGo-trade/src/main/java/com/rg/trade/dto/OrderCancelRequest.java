package com.rg.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 取消订单入参。
 */
@Schema(description = "取消订单")
@Data
public class OrderCancelRequest {

    @Schema(description = "取消原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写取消原因")
    @Size(max = 255, message = "取消原因最多 255 字")
    private String reason;
}
