package com.rg.operations.mapper.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 统计聚合行。
 * <p>
 * 4 条聚合 SQL（订单数、支付额、退款额、新增客户）复用同一个模型，
 * 每条只填自己需要的列，其余为 null，由 Service 按日期归并。
 */
@Data
public class StatAggRow {

    private LocalDate statDate;

    /** 当日新建订单数（含取消） */
    private Long orderCount;

    /** 当日支付成功的订单数 */
    private Long paidOrderCount;

    /** 当日支付成功金额 */
    private BigDecimal salesAmount;

    /** 当日退款完成金额 */
    private BigDecimal refundAmount;

    /** 当日新增会员数 */
    private Long newCustomerCount;
}
