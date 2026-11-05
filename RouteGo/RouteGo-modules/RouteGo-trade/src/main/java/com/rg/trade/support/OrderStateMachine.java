package com.rg.trade.support;

import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.TradeException;
import com.rg.trade.entity.OrderStatusLog;
import com.rg.trade.entity.Orders;
import com.rg.trade.enums.OrderStatus;
import com.rg.trade.mapper.CatalogGatewayMapper;
import com.rg.trade.mapper.CustomerGatewayMapper;
import com.rg.trade.mapper.OrderStatusLogMapper;
import com.rg.trade.mapper.OrdersMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.rg.trade.enums.OrderStatus.CANCELED;
import static com.rg.trade.enums.OrderStatus.COMPLETED;
import static com.rg.trade.enums.OrderStatus.PAID;
import static com.rg.trade.enums.OrderStatus.REFUNDED;
import static com.rg.trade.enums.OrderStatus.REFUNDING;
import static com.rg.trade.enums.OrderStatus.TRAVELING;
import static com.rg.trade.enums.OrderStatus.UNPAID;

/**
 * 订单状态机。
 * <p>
 * 所有状态变更都必须走这里，好处是三件事永远同时发生，不会漏：
 * <ol>
 *   <li>校验流转合法性（非法流转直接 409，不写脏数据）</li>
 *   <li>用乐观锁更新主表，并发重复提交只有一个能成功</li>
 *   <li>追加一条状态流水，并按目标状态触发库存、销量、客户消费的连带更新</li>
 * </ol>
 * 连带更新与主表更新在同一个事务里，任何一步失败整体回滚。
 */
@Component
public class OrderStateMachine {

    /**
     * 合法流转表：key 为当前状态，value 为允许到达的状态。
     * <p>
     * 已支付订单不能直接取消，必须走退款流程，这样资金侧才有据可查。
     */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            UNPAID, EnumSet.of(PAID, CANCELED),
            PAID, EnumSet.of(TRAVELING, REFUNDING),
            TRAVELING, EnumSet.of(COMPLETED),
            REFUNDING, EnumSet.of(REFUNDED, PAID),
            COMPLETED, EnumSet.noneOf(OrderStatus.class),
            CANCELED, EnumSet.noneOf(OrderStatus.class),
            REFUNDED, EnumSet.noneOf(OrderStatus.class)
    );

    private final OrdersMapper ordersMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final CatalogGatewayMapper catalogGatewayMapper;
    private final CustomerGatewayMapper customerGatewayMapper;

    public OrderStateMachine(OrdersMapper ordersMapper,
                             OrderStatusLogMapper statusLogMapper,
                             CatalogGatewayMapper catalogGatewayMapper,
                             CustomerGatewayMapper customerGatewayMapper) {
        this.ordersMapper = ordersMapper;
        this.statusLogMapper = statusLogMapper;
        this.catalogGatewayMapper = catalogGatewayMapper;
        this.customerGatewayMapper = customerGatewayMapper;
    }

    /**
     * 执行一次状态流转。
     *
     * @param order   当前订单（必须带最新 version）
     * @param target  目标状态
     * @param remark  流水备注
     * @param patch   需要一并写入主表的字段（支付时间、取消原因等），可为 null
     */
    @Transactional(rollbackFor = Exception.class)
    public void transition(Orders order, OrderStatus target, String remark, Consumer<Orders> patch) {
        OrderStatus current = OrderStatus.of(order.getStatus());
        if (current == target) {
            throw new TradeException(ErrorCode.CONFLICT, "订单已处于「" + target.getDesc() + "」状态");
        }
        if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new TradeException(ErrorCode.CONFLICT,
                    "订单状态不允许从「" + current.getDesc() + "」变更为「" + target.getDesc() + "」");
        }

        Orders update = new Orders();
        update.setId(order.getId());
        update.setStatus(target.getCode());
        update.setVersion(order.getVersion());
        update.setUpdateBy(CurrentOperator.id());
        if (patch != null) {
            patch.accept(update);
        }
        int rows = ordersMapper.updateById(update);
        if (rows == 0) {
            throw new TradeException(ErrorCode.CONFLICT, "订单已被其他人修改，请刷新后重试");
        }

        applySideEffects(order, target);
        writeLog(order.getId(), current, target, remark);

        // 内存里的对象同步成最新状态，方便调用方继续使用
        order.setStatus(target.getCode());
        order.setVersion(order.getVersion() == null ? 1 : order.getVersion() + 1);
    }

    /**
     * 目标状态带来的连带更新。
     * <p>
     * 注意「已支付」不在这里累加销量与消费额：退款被驳回时订单也会回到「已支付」，
     * 如果按状态触发就会重复计数。累加只在真正收到钱的时候做，见 {@link #onPaid(Orders)}。
     */
    private void applySideEffects(Orders order, OrderStatus target) {
        Integer count = order.getTravelerCount() == null ? 0 : order.getTravelerCount();
        switch (target) {
            case REFUNDED -> {
                if (order.getRouteId() != null && count > 0) {
                    catalogGatewayMapper.decreaseRouteSales(order.getRouteId(), count);
                }
                if (order.getCustomerId() != null && order.getPayAmount() != null) {
                    customerGatewayMapper.decreaseConsume(order.getCustomerId(), order.getPayAmount());
                }
                releaseSeat(order, count);
            }
            case CANCELED -> releaseSeat(order, count);
            default -> {
                // 其余状态不涉及库存与统计
            }
        }
    }

    /**
     * 收到款后的统计累加：线路销量 + 客户累计订单数/消费额。
     * <p>
     * 只在「待支付 → 已支付」时调用一次，重复调用会导致重复计数。
     */
    public void onPaid(Orders order) {
        Integer count = order.getTravelerCount() == null ? 0 : order.getTravelerCount();
        if (order.getRouteId() != null && count > 0) {
            catalogGatewayMapper.increaseRouteSales(order.getRouteId(), count);
        }
        if (order.getCustomerId() != null && order.getPayAmount() != null) {
            customerGatewayMapper.increaseConsume(order.getCustomerId(), order.getPayAmount());
        }
    }

    private void releaseSeat(Orders order, int count) {
        if (order.getDepartureId() != null && count > 0) {
            catalogGatewayMapper.releaseStock(order.getDepartureId(), count);
        }
    }

    /**
     * 写一条状态流水。首条流水（下单）的 fromStatus 为空。
     */
    public void writeLog(Long orderId, OrderStatus from, OrderStatus to, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setFromStatus(from == null ? null : from.getCode());
        log.setToStatus(to.getCode());
        log.setOperatorId(CurrentOperator.id());
        log.setOperatorName(CurrentOperator.name());
        log.setRemark(StringUtils.hasText(remark) ? trim(remark) : to.getDesc());
        log.setCreateTime(LocalDateTime.now());
        statusLogMapper.insert(log);
    }

    private String trim(String value) {
        return value.length() > 255 ? value.substring(0, 255) : value;
    }
}
