package com.rg.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 出发班期新增/编辑入参。
 */
@Schema(description = "出发班期新增/编辑入参")
@Data
public class RouteDepartureSaveRequest {

    @Schema(description = "线路 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择线路")
    private Long routeId;

    @Schema(description = "出发日期，同一线路下不可重复", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择出发日期")
    private LocalDate departDate;

    @Schema(description = "成人价", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请输入成人价")
    @DecimalMin(value = "0.00", message = "成人价不能为负数")
    private BigDecimal adultPrice;

    @Schema(description = "儿童价")
    @DecimalMin(value = "0.00", message = "儿童价不能为负数")
    private BigDecimal childPrice;

    @Schema(description = "库存", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请输入库存")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @Schema(description = "状态：0 停售，1 可售")
    private Byte status;
}
