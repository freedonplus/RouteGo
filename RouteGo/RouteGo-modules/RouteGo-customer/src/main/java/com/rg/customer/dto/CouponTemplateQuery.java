package com.rg.customer.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 优惠券模板分页查询条件。
 */
@Schema(description = "优惠券模板分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class CouponTemplateQuery extends PageQuery {

    @Schema(description = "券名称，模糊匹配")
    private String name;

    @Schema(description = "类型：1 满减，2 折扣")
    private Byte type;

    @Schema(description = "状态：0 停用，1 启用")
    private Byte status;
}
