package com.rg.customer.service;

import com.rg.common.web.PageResult;
import com.rg.customer.dto.CouponQuery;
import com.rg.customer.dto.CouponUseRequest;
import com.rg.customer.vo.CouponVO;

import java.util.List;

/**
 * 用户优惠券服务。
 * <p>
 * 券的生命周期：发放 → 未使用 → 已使用 / 已过期。
 * 只有「未使用且未过期」的券可以核销，核销是条件更新，并发下只会成功一次。
 */
public interface CouponService {

    PageResult<CouponVO> page(CouponQuery query);

    CouponVO detail(Long id);

    /**
     * 某会员的券列表，可按状态过滤；用于会员详情页的券包视图。
     */
    List<CouponVO> listByCustomer(Long customerId, Byte status);

    /**
     * 核销优惠券。传入订单 ID 时会校验订单归属与状态，防止串用。
     */
    void use(Long id, CouponUseRequest request);

    /**
     * 作废优惠券（仅未使用的券），作废后归还模板发行量。
     */
    void revoke(Long id);

    /**
     * 删除券记录（逻辑删除）。
     */
    void delete(Long id);

    /**
     * 批量刷新过期状态，返回本次置为已过期的数量。
     */
    int refreshExpired();
}
