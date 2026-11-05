package com.rg.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新建定制报价入参。
 * <p>
 * 报价单号由服务端生成。一个需求可以有多版报价，但同一时间只允许一版被采纳。
 */
@Schema(description = "新建定制报价")
@Data
public class CustomQuoteCreateRequest {

    @Schema(description = "需求单 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "需求单 ID 不能为空")
    private Long requirementId;

    @Schema(description = "报价金额（元）", requiredMode = Schema.RequiredMode.REQUIRED, example = "6800.00")
    @NotNull(message = "报价金额不能为空")
    @DecimalMin(value = "0.01", message = "报价金额必须大于 0")
    private BigDecimal amount;

    @Schema(description = "报价明细：行程安排、含项与不含项、酒店标准等")
    private String detail;

    @Schema(description = "有效期至（含），过期后由定时任务置为已失效")
    private LocalDate validUntil;
}
