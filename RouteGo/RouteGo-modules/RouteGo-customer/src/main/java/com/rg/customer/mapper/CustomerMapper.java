package com.rg.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.customer.entity.Customer;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 会员 Mapper。
 * <p>
 * 手机号唯一性校验必须把逻辑删除的行也算进去——{@code uk_phone_index} 不包含 deleted 列，
 * 不查回收站就会出现「校验通过但 INSERT 撞唯一键」的 500。
 */
public interface CustomerMapper extends BaseMapper<Customer> {

    @Select("SELECT COUNT(*) FROM customer WHERE phone_index = #{phoneIndex}")
    int countByPhoneIndexIncludingDeleted(@Param("phoneIndex") String phoneIndex);

    @Select("SELECT COUNT(*) FROM customer WHERE phone_index = #{phoneIndex} AND id <> #{id}")
    int countByPhoneIndexExcludingId(@Param("phoneIndex") String phoneIndex, @Param("id") Long id);

    @Select("SELECT * FROM customer WHERE phone_index = #{phoneIndex} AND deleted = 0 LIMIT 1")
    Customer selectByPhoneIndex(@Param("phoneIndex") String phoneIndex);

    @Select("SELECT COUNT(*) FROM customer WHERE openid = #{openid} AND deleted = 0")
    int countByOpenid(@Param("openid") String openid);

    /**
     * 积分增减，结果不低于 0，避免运营扣成负分。
     */
    @Update("""
            UPDATE customer
            SET points = GREATEST(points + #{delta}, 0)
            WHERE id = #{id} AND deleted = 0
            """)
    int adjustPoints(@Param("id") Long id, @Param("delta") int delta);
}
