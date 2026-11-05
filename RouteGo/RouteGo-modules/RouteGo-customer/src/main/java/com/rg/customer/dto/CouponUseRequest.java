package com.rg.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 优惠券核销入参。
 * <p>
 * 传入订单号时，服务端会校验该订单属于当前会员且未取消/未退款，
 * 保证券不会被挪用到别人的订单上。
 */
@Schema(description = "优惠券核销入参")
@Data
public class CouponUseRequest {

    @Schema(description = "使用的订单 ID，可空；填写后会校验订单归属与状态")
    private Long orderId;
}
