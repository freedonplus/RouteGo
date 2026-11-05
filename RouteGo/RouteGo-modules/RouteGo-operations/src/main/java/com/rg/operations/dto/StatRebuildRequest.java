package com.rg.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 统计重算入参。
 * <p>
 * 重算按天覆盖写入（stat_daily.stat_date 唯一），可以反复执行，结果一致；
 * 单次最多 366 天，避免误传一个十年区间把库拖垮。
 */
@Schema(description = "经营统计重算")
@Data
public class StatRebuildRequest {

    @Schema(description = "重算起始日期（含）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "起始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "重算结束日期（含）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;
}
