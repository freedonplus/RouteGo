package com.rg.customer.mapper;

import com.rg.customer.mapper.model.OrderSnapshot;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 客户域对 trade 域的只读网关。
 * <p>
 * 只做两件事：核销优惠券时确认订单归属与状态；券列表里补订单号。
 * 订单的写入与状态流转仍然只在 trade 域内，跨域不反向修改。
 */
public interface OrderGatewayMapper {

    @Select("""
            SELECT id, order_no, customer_id, status, pay_amount
            FROM orders
            WHERE id = #{orderId} AND deleted = 0
            """)
    OrderSnapshot selectOrder(@Param("orderId") Long orderId);

    @Select("""
            <script>
            SELECT id, order_no, customer_id, status, pay_amount
            FROM orders
            WHERE deleted = 0 AND id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    java.util.List<OrderSnapshot> selectOrders(@Param("ids") java.util.Collection<Long> ids);
}
