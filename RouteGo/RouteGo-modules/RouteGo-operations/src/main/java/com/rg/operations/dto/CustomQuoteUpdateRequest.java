package com.rg.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 编辑定制报价入参。
 * <p>
 * 只有「待确认」的报价可以改；已采纳、已失效的报价是成交留痕，改动会让对账失去依据，
 * 需要调价请新建一版报价。
 */
@Schema(description = "编辑定制报价")
@Data
public class CustomQuoteUpdateRequest {

    @Schema(description = "报价金额（元）")
    @DecimalMin(value = "0.01", message = "报价金额必须大于 0")
    private BigDecimal amount;

    @Schema(description = "报价明细")
    private String detail;

    @Schema(description = "有效期至（含）")
    private LocalDate validUntil;
}
