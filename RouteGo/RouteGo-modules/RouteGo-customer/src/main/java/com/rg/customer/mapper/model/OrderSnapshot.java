package com.rg.customer.mapper.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单快照读模型：核销优惠券时校验订单归属与状态。
 */
@Data
public class OrderSnapshot {

    private Long id;
    private String orderNo;
    private Long customerId;
    private Byte status;
    private BigDecimal payAmount;
}
