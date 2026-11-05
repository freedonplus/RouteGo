package com.rg.customer.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户优惠券分页查询条件。
 */
@Schema(description = "用户优惠券分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class CouponQuery extends PageQuery {

    @Schema(description = "会员 ID")
    private Long customerId;

    @Schema(description = "券模板 ID")
    private Long templateId;

    @Schema(description = "券码，精确匹配")
    private String code;

    @Schema(description = "状态：0 未使用，1 已使用，2 已过期")
    private Byte status;

    @Schema(description = "领取时间起（含）")
    private LocalDateTime receiveTimeStart;

    @Schema(description = "领取时间止（含）")
    private LocalDateTime receiveTimeEnd;
}
