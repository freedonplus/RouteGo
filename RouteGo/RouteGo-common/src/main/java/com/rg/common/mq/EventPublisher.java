package com.rg.common.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;

/**
 * 统一事件发布器。
 * <p>
 * 自动完成两件事：包装 {@link MessageEnvelope}、序列化为 JSON。
 * 业务侧只需要关心 topic / tag / payload。
 */
public class EventPublisher {

    private static final long DEFAULT_SEND_TIMEOUT_MILLIS = 3000L;

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(RocketMQTemplate rocketMQTemplate, ObjectMapper objectMapper) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 同步发送，返回事件 ID。
     */
    public String publish(String topic, String tag, Object payload) {
        MessageEnvelope<Object> envelope = MessageEnvelope.of(payload);
        rocketMQTemplate.syncSend(destination(topic, tag), toMessage(envelope), DEFAULT_SEND_TIMEOUT_MILLIS);
        return envelope.getEventId();
    }

    /**
     * 异步发送，返回的事件 ID 在 future 完成时给出。
     */
    public CompletableFuture<String> publishAsync(String topic, String tag, Object payload) {
        MessageEnvelope<Object> envelope = MessageEnvelope.of(payload);
        CompletableFuture<String> future = new CompletableFuture<>();
        rocketMQTemplate.asyncSend(destination(topic, tag), toMessage(envelope), new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                future.complete(envelope.getEventId());
            }

            @Override
            public void onException(Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    /**
     * 顺序消息：同一个 hashKey 会进入同一队列，适合订单状态类事件。
     */
    public String publishOrderly(String topic, String tag, Object payload, String hashKey) {
        MessageEnvelope<Object> envelope = MessageEnvelope.of(payload);
        rocketMQTemplate.syncSendOrderly(destination(topic, tag), toMessage(envelope), hashKey);
        return envelope.getEventId();
    }

    private Message<String> toMessage(MessageEnvelope<?> envelope) {
        try {
            String json = objectMapper.writeValueAsString(envelope);
            return MessageBuilder.withPayload(json)
                    .setHeader(RocketMqConstants.EVENT_ID_HEADER, envelope.getEventId())
                    .setHeader(RocketMqConstants.EVENT_TYPE_HEADER, envelope.getEventType())
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("RocketMQ 事件序列化失败", e);
        }
    }

    private String destination(String topic, String tag) {
        return StringUtils.hasText(tag) ? topic + ":" + tag : topic;
    }
}
