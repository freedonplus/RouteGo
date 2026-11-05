package com.rg.trade.mapper;

import com.rg.trade.mapper.model.CustomerSnapshot;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 交易域对 customer 域的读模型与消费统计更新。
 * <p>
 * 客户的注册、登录、资料维护仍然只在 customer 域内，交易域只做两件事：
 * 下单前确认客户可用、支付/退款后回写累计订单数与消费金额。
 */
public interface CustomerGatewayMapper {

    @Select("""
            SELECT id, nickname, phone, phone_index, status
            FROM customer
            WHERE id = #{customerId} AND deleted = 0
            """)
    CustomerSnapshot selectCustomer(@Param("customerId") Long customerId);

    @Select("""
            SELECT id, nickname, phone, phone_index, status
            FROM customer
            WHERE phone_index = #{phoneIndex} AND deleted = 0
            """)
    CustomerSnapshot selectCustomerByPhoneIndex(@Param("phoneIndex") String phoneIndex);

    /**
     * 批量取客户，供订单列表一次性补昵称与手机号，避免 N+1。
     */
    @Select("""
            <script>
            SELECT id, nickname, phone, phone_index, status
            FROM customer
            WHERE deleted = 0 AND id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    java.util.List<CustomerSnapshot> selectCustomers(@Param("ids") java.util.Collection<Long> ids);

    /**
     * 支付成功后累加消费额与订单数。
     */
    @Update("""
            UPDATE customer
            SET order_count = order_count + 1,
                total_amount = total_amount + #{amount}
            WHERE id = #{customerId} AND deleted = 0
            """)
    int increaseConsume(@Param("customerId") Long customerId, @Param("amount") BigDecimal amount);

    /**
     * 退款完成后回退消费额与订单数。
     */
    @Update("""
            UPDATE customer
            SET order_count = GREATEST(order_count - 1, 0),
                total_amount = GREATEST(total_amount - #{amount}, 0.00)
            WHERE id = #{customerId} AND deleted = 0
            """)
    int decreaseConsume(@Param("customerId") Long customerId, @Param("amount") BigDecimal amount);
}
