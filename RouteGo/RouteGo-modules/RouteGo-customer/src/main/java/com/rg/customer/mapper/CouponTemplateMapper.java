package com.rg.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.customer.entity.CouponTemplate;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券模板 Mapper。
 * <p>
 * 发券时用条件 UPDATE 原子占用发行量：{@code total_count = 0 OR received_count < total_count}。
 * 并发发放只有一条能命中，另一条拿到 0 行即视为「已领完」，
 * 这样不需要行锁也不会超发。
 */
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    @Update("""
            UPDATE coupon_template
            SET received_count = received_count + 1
            WHERE id = #{templateId}
              AND deleted = 0
              AND (total_count = 0 OR received_count < total_count)
            """)
    int increaseReceived(@Param("templateId") Long templateId);
}
