package com.rg.operations.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.operations.entity.StatDaily;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 每日经营统计 Mapper。
 * <p>
 * 重算按天覆盖写：{@code stat_date} 上有唯一索引，用 ON DUPLICATE KEY UPDATE 实现幂等
 * upsert，重复跑同一区间结果一致，不会产生重复行。
 */
public interface StatDailyMapper extends BaseMapper<StatDaily> {

    @Insert("""
            INSERT INTO stat_daily
                (stat_date, order_count, paid_order_count, sales_amount, refund_amount, new_customer_count,
                 create_time, update_time)
            VALUES
                (#{statDate}, #{orderCount}, #{paidOrderCount}, #{salesAmount}, #{refundAmount}, #{newCustomerCount},
                 NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                order_count        = VALUES(order_count),
                paid_order_count   = VALUES(paid_order_count),
                sales_amount       = VALUES(sales_amount),
                refund_amount      = VALUES(refund_amount),
                new_customer_count = VALUES(new_customer_count),
                update_time        = NOW()
            """)
    int upsert(@Param("statDate") LocalDate statDate,
               @Param("orderCount") int orderCount,
               @Param("paidOrderCount") int paidOrderCount,
               @Param("salesAmount") BigDecimal salesAmount,
               @Param("refundAmount") BigDecimal refundAmount,
               @Param("newCustomerCount") int newCustomerCount);
}
