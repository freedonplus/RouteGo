package com.rg.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.trade.entity.Orders;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 订单主表 Mapper。
 * <p>
 * 订单号是业务主键，唯一索引不区分逻辑删除，因此生成订单号时要把回收站里的行也
 * 算进来，避免复用旧号在 INSERT 阶段撞唯一键。
 */
public interface OrdersMapper extends BaseMapper<Orders> {

    @Select("SELECT COUNT(*) FROM orders WHERE order_no = #{orderNo}")
    long countByOrderNoIncludingDeleted(@Param("orderNo") String orderNo);

    /**
     * 统计某客户未完结订单数，用于客户停用/删除前的前置校验。
     */
    @Select("""
            SELECT COUNT(*) FROM orders
            WHERE customer_id = #{customerId} AND deleted = 0 AND status IN (0, 1, 2, 5)
            """)
    long countUnfinishedByCustomer(@Param("customerId") Long customerId);
}
