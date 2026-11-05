package com.rg.operations.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 每日经营统计分页查询条件。
 */
@Schema(description = "每日统计查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class StatDailyQuery extends PageQuery {

    @Schema(description = "统计日期起（含）")
    private LocalDate startDate;

    @Schema(description = "统计日期止（含）")
    private LocalDate endDate;
}
