package com.rg.catalog.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 线路评价分页查询条件。
 */
@Schema(description = "线路评价分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class RouteReviewQuery extends PageQuery {

    @Schema(description = "线路 ID")
    private Long routeId;

    @Schema(description = "评分 1-5")
    private Byte rating;

    @Schema(description = "状态：0 隐藏，1 显示")
    private Byte status;
}
