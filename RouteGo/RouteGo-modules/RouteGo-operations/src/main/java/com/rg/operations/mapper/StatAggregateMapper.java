package com.rg.operations.mapper;

import com.rg.operations.mapper.model.StatAggRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 经营统计的聚合查询。
 * <p>
 * 口径说明（与报表页文案保持一致）：
 * <ul>
 *   <li>订单数：按 {@code orders.create_time} 归属日期，包含当日已取消的订单。</li>
 *   <li>已支付订单数、销售额：按 {@code payment_record.pay_time} 归属日期，只算 status = 1（成功）。
 *       一笔订单分多次支付时按流水累加，这与「实收」口径一致。</li>
 *   <li>退款金额：按 {@code refund_record.update_time} 归属日期，只算 status = 3（已退款）。
 *       表上没有独立的「退款完成时间」，用最后更新时间近似，退款单终态后不会再被改写。</li>
 *   <li>新增客户：按 {@code customer.register_time} 归属日期，只算未删除的会员。</li>
 * </ul>
 * 区间查询用左闭右开 {@code [start, end + 1)}，避免 {@code DATE(column)} 让索引失效。
 */
public interface StatAggregateMapper {

    @Select("""
            SELECT DATE(create_time) AS stat_date, COUNT(*) AS order_count
            FROM orders
            WHERE deleted = 0 AND create_time >= #{start} AND create_time < #{endExclusive}
            GROUP BY DATE(create_time)
            """)
    List<StatAggRow> countOrders(@Param("start") LocalDateTime start,
                                 @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            SELECT DATE(pay_time) AS stat_date,
                   COUNT(DISTINCT order_id) AS paid_order_count,
                   SUM(amount) AS sales_amount
            FROM payment_record
            WHERE status = 1 AND pay_time >= #{start} AND pay_time < #{endExclusive}
            GROUP BY DATE(pay_time)
            """)
    List<StatAggRow> sumPaid(@Param("start") LocalDateTime start,
                             @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            SELECT DATE(update_time) AS stat_date, SUM(amount) AS refund_amount
            FROM refund_record
            WHERE deleted = 0 AND status = 3 AND update_time >= #{start} AND update_time < #{endExclusive}
            GROUP BY DATE(update_time)
            """)
    List<StatAggRow> sumRefund(@Param("start") LocalDateTime start,
                               @Param("endExclusive") LocalDateTime endExclusive);

    @Select("""
            SELECT DATE(register_time) AS stat_date, COUNT(*) AS new_customer_count
            FROM customer
            WHERE deleted = 0 AND register_time >= #{start} AND register_time < #{endExclusive}
            GROUP BY DATE(register_time)
            """)
    List<StatAggRow> countNewCustomers(@Param("start") LocalDateTime start,
                                       @Param("endExclusive") LocalDateTime endExclusive);

    /**
     * 校验日期区间内是否已有明细数据，用于提示「重算区间超出数据范围」。
     */
    @Select("""
            SELECT COUNT(*) FROM orders
            WHERE deleted = 0 AND create_time >= #{start} AND create_time < #{endExclusive}
            """)
    long countOrdersInRange(@Param("start") LocalDateTime start,
                            @Param("endExclusive") LocalDateTime endExclusive);

    /**
     * 统计区间内的会员注册日期边界，便于提示最早数据日期。
     */
    @Select("""
            SELECT MIN(DATE(register_time)) FROM customer WHERE deleted = 0
            """)
    LocalDate minCustomerDate();
}
