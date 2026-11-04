package com.rg.common.mq;

/**
 * RocketMQ 统一约定。
 * <p>
 * Topic 建议命名为 {@code RouteGo-<业务域>-<事件>}，例如
 * {@code RouteGo-order-paid}；消费组建议 {@code RouteGo-<服务>-<用途>}。
 */
public final class RocketMqConstants {

    public static final String EVENT_ID_HEADER = "eventId";
    public static final String EVENT_TYPE_HEADER = "eventType";

    public static final String TOPIC_PREFIX = "RouteGo-";
    public static final String CONSUMER_GROUP_PREFIX = "RouteGo-";

    /** 订单支付成功事件（trade 生产，catalog/customer/operations 消费） */
    public static final String TOPIC_ORDER_PAID = TOPIC_PREFIX + "order-paid";

    /** 退款成功事件（trade 生产） */
    public static final String TOPIC_ORDER_REFUNDED = TOPIC_PREFIX + "order-refunded";

    /** 权限/角色变更事件（admin 生产，各服务消费后清理本地缓存） */
    public static final String TOPIC_PERMISSION_CHANGED = TOPIC_PREFIX + "permission-changed";

    private RocketMqConstants() {
    }
}
