package com.rg.operations.mapper.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单只读快照，用于工单关联展示与统计口径校验。
 */
@Data
public class OrderSnapshot {

    private Long id;

    private String orderNo;

    private Long customerId;

    private Byte status;

    private BigDecimal payAmount;
}
