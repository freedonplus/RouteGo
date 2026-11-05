package com.rg.catalog.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 出发班期分页查询条件。
 */
@Schema(description = "出发班期分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class RouteDepartureQuery extends PageQuery {

    @Schema(description = "线路 ID")
    private Long routeId;

    @Schema(description = "状态：0 停售，1 可售")
    private Byte status;

    @Schema(description = "出发日期起（含）", example = "2026-11-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Schema(description = "出发日期止（含）", example = "2026-11-30")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
