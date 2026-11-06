package com.rg.customer.mapper.model;

import lombok.Data;

/**
 * 会员可用券数量（按 customer_id 分组统计）。
 * <p>
 * 列表页要显示「可用券」列，逐个会员查一次 count 会变成 N+1 查询，
 * 所以按当前页的会员 ID 一次性 group by 拿回来。
 */
@Data
public class CustomerCouponCount {

    private Long customerId;

    private Long total;
}
