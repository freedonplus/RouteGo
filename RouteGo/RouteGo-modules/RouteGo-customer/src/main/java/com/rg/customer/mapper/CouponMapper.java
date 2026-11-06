package com.rg.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.customer.entity.Coupon;
import com.rg.customer.mapper.model.CustomerCouponCount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 用户优惠券 Mapper。
 * <p>
 * 核销与过期刷新都用条件 UPDATE 完成，避免「查出来判断再更新」的并发窗口：
 * 两个人同时核销同一张券，只有一条 UPDATE 会命中。
 */
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 核销：未使用 → 已使用。返回 0 表示券已被使用、已过期或不存在。
     */
    @Update("""
            UPDATE coupon
            SET status = 1, use_time = #{useTime}, order_id = #{orderId}
            WHERE id = #{id} AND status = 0 AND deleted = 0
            """)
    int markUsed(@Param("id") Long id,
                 @Param("orderId") Long orderId,
                 @Param("useTime") LocalDateTime useTime);

    /**
     * 作废：把券标记为已过期并释放模板占用（仅未使用的券可作废）。
     */
    @Update("""
            UPDATE coupon
            SET status = 2
            WHERE id = #{id} AND status = 0 AND deleted = 0
            """)
    int markExpired(@Param("id") Long id);

    /**
     * 批量刷新过期状态：按模板的两种有效期规则算出过期时间，把已过期但仍标记为未使用的券置为已过期。
     *
     * @return 本次被置为已过期的券数量
     */
    @Update("""
            UPDATE coupon c
            JOIN coupon_template t ON t.id = c.template_id
            SET c.status = 2
            WHERE c.deleted = 0
              AND c.status = 0
              AND (
                    (t.valid_type = 1 AND t.end_time IS NOT NULL AND t.end_time < NOW())
                 OR (t.valid_type = 2 AND t.valid_days IS NOT NULL
                     AND DATE_ADD(c.receive_time, INTERVAL t.valid_days DAY) < NOW())
              )
            """)
    int expireOutdated();

    /**
     * 券码唯一性校验。uk_code 不含 deleted 列，所以必须把逻辑删除的行一起数进来。
     */
    @Select("SELECT COUNT(*) FROM coupon WHERE code = #{code}")
    int countByCodeIncludingDeleted(@Param("code") String code);

    /**
     * 会员被删除时，把其名下未使用的券一并置为已过期。
     */
    @Update("""
            UPDATE coupon
            SET status = 2
            WHERE customer_id = #{customerId} AND status = 0 AND deleted = 0
            """)
    int expireUnusedByCustomer(@Param("customerId") Long customerId);

    @Select("""
            SELECT COUNT(*) FROM coupon
            WHERE deleted = 0 AND customer_id = #{customerId} AND status = #{status}
            """)
    long countByCustomerAndStatus(@Param("customerId") Long customerId, @Param("status") int status);

    /**
     * 批量统计一批会员的可用券数量，列表页一次查完，避免逐个会员 count 的 N+1。
     */
    @Select("""
            <script>
            SELECT customer_id AS customerId, COUNT(*) AS total
            FROM coupon
            WHERE deleted = 0 AND status = 0
              AND customer_id IN
            <foreach collection="customerIds" item="cid" open="(" separator="," close=")">#{cid}</foreach>
            GROUP BY customer_id
            </script>
            """)
    List<CustomerCouponCount> countAvailableByCustomers(@Param("customerIds") Collection<Long> customerIds);

    /**
     * 统计某模板下已发出的券数量（含已使用与已过期），用于阻止删除有发券记录的模板。
     */
    @Select("SELECT COUNT(*) FROM coupon WHERE template_id = #{templateId} AND deleted = 0")
    long countByTemplate(@Param("templateId") Long templateId);

    @Select("""
            <script>
            SELECT COUNT(*) FROM coupon
            WHERE deleted = 0 AND template_id = #{templateId}
              AND customer_id IN
            <foreach collection="customerIds" item="cid" open="(" separator="," close=")">#{cid}</foreach>
            </script>
            """)
    long countByTemplateAndCustomers(@Param("templateId") Long templateId,
                                     @Param("customerIds") Collection<Long> customerIds);
}
