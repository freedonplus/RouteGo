package com.rg.trade.service.impl;

import com.rg.trade.entity.Orders;
import com.rg.trade.mapper.OrdersMapper;
import com.rg.trade.service.OrdersService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单主表 服务实现类
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-03
 */
@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

}
