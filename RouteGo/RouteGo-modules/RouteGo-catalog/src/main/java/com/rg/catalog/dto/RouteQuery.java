package com.rg.catalog.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 线路分页查询条件。
 */
@Schema(description = "线路分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class RouteQuery extends PageQuery {

    @Schema(description = "线路标题，模糊匹配")
    private String title;

    @Schema(description = "线路编号，模糊匹配")
    private String routeNo;

    @Schema(description = "目的地，模糊匹配")
    private String destination;

    @Schema(description = "出发城市，模糊匹配")
    private String departureCity;

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "状态：0 下架，1 上架")
    private Byte status;
}
