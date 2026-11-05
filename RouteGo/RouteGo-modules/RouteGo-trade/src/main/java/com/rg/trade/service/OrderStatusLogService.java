package com.rg.trade.service;

import com.rg.common.web.PageResult;
import com.rg.trade.dto.OrderStatusLogQuery;
import com.rg.trade.vo.OrderStatusLogVO;

import java.util.List;

/**
 * 订单状态流水服务（追加型，不提供修改与删除）。
 */
public interface OrderStatusLogService {

    PageResult<OrderStatusLogVO> page(OrderStatusLogQuery query);

    List<OrderStatusLogVO> listByOrder(Long orderId);
}
