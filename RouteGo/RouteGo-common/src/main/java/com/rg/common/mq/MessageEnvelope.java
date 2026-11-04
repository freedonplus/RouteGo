package com.rg.common.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 统一消息信封。
 * <p>
 * 所有业务事件都以该结构发送，消费方拿到的是可追溯、可幂等的事件，
 * 而不是裸 JSON。{@code eventId} 可直接作为消费端幂等键。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageEnvelope<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件唯一 ID，消费端可用于幂等 */
    private String eventId;

    /** 事件类型，默认取 payload 的简单类名 */
    private String eventType;

    /** 事件发生时间 */
    private LocalDateTime occurredAt;

    /** 业务载荷 */
    private T payload;

    public static <T> MessageEnvelope<T> of(T payload) {
        return new MessageEnvelope<>(
                UUID.randomUUID().toString().replace("-", ""),
                payload == null ? "unknown" : payload.getClass().getSimpleName(),
                LocalDateTime.now(),
                payload);
    }
}
