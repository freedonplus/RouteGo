package com.rg.common.mq;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.core.RocketMQListener;

/**
 * 消费端基类：负责反序列化 {@link MessageEnvelope}，把业务异常交给
 * RocketMQ 重试。
 * <p>
 * 用法：
 * <pre>{@code
 * @Component
 * @RocketMQMessageListener(topic = RocketMqConstants.TOPIC_ORDER_PAID,
 *         consumerGroup = "RouteGo-catalog-order-paid")
 * public class OrderPaidListener extends AbstractRouteGoListener<OrderPaidEvent> {
 *     public OrderPaidListener(ObjectMapper mapper) {
 *         super(OrderPaidEvent.class, mapper);
 *     }
 *     @Override
 *     protected void handle(MessageEnvelope<OrderPaidEvent> envelope) {
 *         // 幂等：用 envelope.getEventId() 去重
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractRouteGoListener<T> implements RocketMQListener<String> {

    private final Class<T> payloadType;
    private final ObjectMapper objectMapper;

    protected AbstractRouteGoListener(Class<T> payloadType, ObjectMapper objectMapper) {
        this.payloadType = payloadType;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(String message) {
        MessageEnvelope<T> envelope;
        try {
            JavaType type = objectMapper.getTypeFactory()
                    .constructParametricType(MessageEnvelope.class, payloadType);
            envelope = objectMapper.readValue(message, type);
        } catch (Exception e) {
            throw new IllegalStateException("RocketMQ 消息反序列化失败: " + message, e);
        }
        handle(envelope);
    }

    protected abstract void handle(MessageEnvelope<T> envelope);
}
