package com.rg.trade.service;

import com.rg.common.web.PageResult;
import com.rg.trade.dto.OrderCancelRequest;
import com.rg.trade.dto.OrderCreateRequest;
import com.rg.trade.dto.OrderPayRequest;
import com.rg.trade.dto.OrderQuery;
import com.rg.trade.dto.OrderUpdateRequest;
import com.rg.trade.vo.OrderVO;

/**
 * 订单服务。
 * <p>
 * 状态流转全部经由 {@link com.rg.trade.support.OrderStateMachine}，
 * 这里只负责参数校验、快照落库与查询组装。
 */
public interface OrderService {

    PageResult<OrderVO> page(OrderQuery query);

    /**
     * 订单详情，附带出行人、状态流水、支付与退款记录。
     */
    OrderVO detail(Long id);

    /**
     * 后台代录订单：生成快照、占用班期库存、写首条状态流水。
     */
    Long create(OrderCreateRequest request);

    /**
     * 修改联系人与备注。金额、线路、出发日期属于快照，不在此修改。
     */
    void update(Long id, OrderUpdateRequest request);

    /**
     * 取消订单，仅「待支付」可取消，取消后回补库存。
     */
    void cancel(Long id, OrderCancelRequest request);

    /**
     * 支付订单，仅「待支付」可支付；成功后累加线路销量与客户消费。
     */
    void pay(Long id, OrderPayRequest request);

    /**
     * 开始行程：已支付 → 出行中。
     */
    void startTravel(Long id);

    /**
     * 完成行程：出行中 → 已完成。
     */
    void complete(Long id);

    /**
     * 逻辑删除订单，仅终态（已完成/已取消/已退款）允许删除。
     */
    void delete(Long id);
}
