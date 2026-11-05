package com.rg.catalog.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 标签分页查询条件。
 */
@Schema(description = "线路标签分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class RouteTagQuery extends PageQuery {

    @Schema(description = "标签名称，模糊匹配")
    private String tagName;

    @Schema(description = "状态：0 禁用，1 启用")
    private Byte status;
}
