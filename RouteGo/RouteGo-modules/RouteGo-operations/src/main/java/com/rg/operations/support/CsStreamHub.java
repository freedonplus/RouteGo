package com.rg.operations.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rg.operations.mapper.CsTicketMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 客服消息推送中心（SSE）。
 * <p>
 * 每个在线客服持有一条 {@code text/event-stream} 长连接，客户发消息、客服读消息等变更
 * 通过这里广播，前端不再靠定时器轮询。客服场景是单向的：服务端推「有新消息」，
 * 「发消息」本来就是 POST，因此用 SSE 而非 WebSocket，省掉双向通道的心跳与协议开销。
 * <p>
 * 几个刻意的设计：
 * <ul>
 *   <li>连接永不主动超时（timeout=0），靠 20 秒心跳保活并探测死连接——nginx 默认
 *       60 秒读超时，心跳间隔必须小于它，否则代理会先掐断；</li>
 *   <li>一个人可开多个标签页，所以按操作人 ID 存一组 emitter；</li>
 *   <li>广播只序列化一次，避免每个连接各转一遍 JSON；</li>
 *   <li>推送统一在所有在线客服间广播，由前端按 ticketId 自行过滤——客服是个位数，
 *       广播成本可忽略，而这样能顺带让列表页的未读总数实时更新。</li>
 * </ul>
 */
@Component
public class CsStreamHub {

    private static final Logger log = LoggerFactory.getLogger(CsStreamHub.class);

    /** 心跳周期：必须小于 nginx 的 proxy_read_timeout（默认 60 秒） */
    private static final long HEARTBEAT_SECONDS = 20L;

    /** 事件名，前端 EventSource 用 addEventListener('cs', ...) 订阅 */
    public static final String EVENT_CS = "cs";
    /** 建连事件，携带当前未读总数，前端不必再单独拉一次统计 */
    public static final String EVENT_HELLO = "hello";

    private final Map<Long, Set<SseEmitter>> connections = new ConcurrentHashMap<>();
    private final CsTicketMapper ticketMapper;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService heartbeat;

    public CsStreamHub(CsTicketMapper ticketMapper, ObjectMapper objectMapper) {
        this.ticketMapper = ticketMapper;
        this.objectMapper = objectMapper;
        this.heartbeat = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "cs-stream-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        this.heartbeat.scheduleWithFixedDelay(this::broadcastHeartbeat,
                HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 建立一条推送连接。
     *
     * @param operatorId 后台操作人 ID
     */
    public SseEmitter open(Long operatorId) {
        SseEmitter emitter = new SseEmitter(0L);
        connections.computeIfAbsent(operatorId, key -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> remove(operatorId, emitter));
        emitter.onTimeout(() -> remove(operatorId, emitter));
        emitter.onError(throwable -> remove(operatorId, emitter));

        // 建连即推一份当前未读，前端首屏直接有值，少一次请求
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("staffUnread", ticketMapper.countStaffUnread());
            payload.put("time", LocalDateTime.now().toString());
            write(operatorId, emitter, EVENT_HELLO, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            remove(operatorId, emitter);
        }
        log.debug("SSE 连接建立，操作人 {}，当前在线 {} 人", operatorId, connections.size());
        return emitter;
    }

    /**
     * 广播一条工单变更。没有在线连接时直接返回，不做无意义的查询与序列化。
     *
     * @param type       变更类型：message 有新消息，read 未读被清零
     * @param ticketId   工单 ID
     * @param senderType 消息发送方：1 客户，2 客服；read 事件为 null
     * @param preview    消息摘要，用于通知文案
     */
    public void publish(String type, Long ticketId, Integer senderType, String preview) {
        broadcast(type, ticketId, senderType, preview);
    }

    /**
     * 事务提交后再广播。
     * <p>
     * 推送是「让前端来拉数据」的信号，如果抢在事务提交前发出去，前端很可能读到旧数据
     * 而看不到刚发的那条消息，表现为「提示有了但内容没刷新」。
     */
    public void publishAfterCommit(String type, Long ticketId, Integer senderType, String preview) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(type, ticketId, senderType, preview);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(type, ticketId, senderType, preview);
            }
        });
    }

    /** 当前在线连接数，供运维端点与排查用 */
    public int onlineConnections() {
        return connections.values().stream().mapToInt(Set::size).sum();
    }

    private void broadcast(String type, Long ticketId, Integer senderType, String preview) {
        if (connections.isEmpty()) {
            return;
        }
        String json;
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", type);
            payload.put("ticketId", ticketId);
            payload.put("senderType", senderType);
            payload.put("preview", preview);
            payload.put("staffUnread", ticketMapper.countStaffUnread());
            payload.put("time", LocalDateTime.now().toString());
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("客服推送序列化失败，本次跳过：{}", e.getMessage());
            return;
        }
        connections.forEach((operatorId, emitters) ->
                emitters.forEach(emitter -> write(operatorId, emitter, EVENT_CS, json)));
    }

    private void broadcastHeartbeat() {
        if (connections.isEmpty()) {
            return;
        }
        // 注释帧不算事件，EventSource 不会触发回调，只用来占住连接防代理超时
        connections.forEach((operatorId, emitters) ->
                emitters.forEach(emitter -> write(operatorId, emitter, null, "ping")));
    }

    /**
     * 写一条事件。任何异常都意味着这条连接已经不可用（客户端关页面、网络中断、代理掐断），
     * 直接摘掉，避免注册表越积越大。
     */
    private void write(Long operatorId, SseEmitter emitter, String eventName, String data) {
        try {
            if (eventName == null) {
                // 心跳走注释帧
                emitter.send(SseEmitter.event().comment(data));
                return;
            }
            emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            remove(operatorId, emitter);
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // 已经断了，complete 报错无所谓
            }
        }
    }

    private void remove(Long operatorId, SseEmitter emitter) {
        Set<SseEmitter> emitters = connections.get(operatorId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            // 只有集合确实空了才移除 key，避免并发下把刚建立的新连接一起丢掉
            connections.remove(operatorId, emitters);
        }
    }

    /**
     * 停机时主动断开所有连接。
     * <p>
     * 长连接会把优雅停机的宽限期（spring.lifecycle.timeout-per-shutdown-phase）耗满，
     * 每次重启都卡 20 秒。这里主动 complete，前端收到断开后按退避重连，新实例起来就能接上。
     */
    @PreDestroy
    public void shutdown() {
        heartbeat.shutdownNow();
        int closed = onlineConnections();
        connections.forEach((operatorId, emitters) -> emitters.forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // 停机阶段忽略
            }
        }));
        connections.clear();
        if (closed > 0) {
            log.info("停机：已断开 {} 条客服推送连接", closed);
        }
    }
}
