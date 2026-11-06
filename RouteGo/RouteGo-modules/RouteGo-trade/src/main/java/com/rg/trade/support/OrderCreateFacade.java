package com.rg.trade.support;

import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.TradeException;
import com.rg.common.redis.DistributedLock;
import com.rg.common.redis.RedisService;
import com.rg.trade.dto.OrderCreateRequest;
import com.rg.trade.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 下单门面：幂等占位 + 班期维度的分布式锁，再交给 {@link OrderService} 落库。
 * <p>
 * <b>为什么必须在这一层做？</b>
 * <ul>
 *   <li>{@code OrderService.create} 带 {@code @Transactional}。若把锁写在事务方法内部，
 *       unlock 会发生在事务提交之前，其他线程可能读到未提交的已售数量，
 *       再配合扣减逻辑就会出现判断与事实不符。放在门面层，锁包住的是
 *       「事务开始 → 提交结束」的完整区间，释放锁时数据一定可见。</li>
 *   <li>占位与结果回写必须在业务异常时回滚（删除占位键），否则一次失败会让用户
 *       在 TTL 内都无法重试。</li>
 * </ul>
 * <p>
 * <b>和原子 UPDATE 的关系</b>：{@code CatalogGatewayMapper.occupyStock} 本身已经是
 * {@code stock - sold >= count} 的条件更新，不会超卖。这里的锁不承担"防超卖"职责，
 * 它只做两件事：把同一班期的并发写串行化，减少大量 UPDATE 命中 0 行导致的事务回滚；
 * 保证「幂等键 → 订单号」的写回顺序与请求顺序一致。
 */
@Component
public class OrderCreateFacade {

    private static final Logger log = LoggerFactory.getLogger(OrderCreateFacade.class);

    /** 占位有效期：足够覆盖一次下单的最长耗时，超时视为放弃 */
    private static final Duration PENDING_TTL = Duration.ofMinutes(2);
    /** 结果有效期：客户端在这段时间内用同一幂等键重试，直接拿到同一张订单 */
    private static final Duration RESULT_TTL = Duration.ofMinutes(30);
    /** 未传幂等键时的去重窗口：同一会员对同一班期同人数，30 秒内只认一单 */
    private static final Duration DUP_WINDOW = Duration.ofSeconds(30);

    private static final String DONE_PREFIX = "DONE:";
    private static final String PENDING = "PENDING";

    private static final long LOCK_WAIT_SECONDS = 2;
    private static final long LOCK_LEASE_SECONDS = 15;

    private final OrderService orderService;
    private final DistributedLock distributedLock;
    private final RedisService redisService;

    public OrderCreateFacade(OrderService orderService,
                             DistributedLock distributedLock,
                             RedisService redisService) {
        this.orderService = orderService;
        this.distributedLock = distributedLock;
        this.redisService = redisService;
    }

    /**
     * 创建订单。
     *
     * @param request        下单参数
     * @param idempotencyKey 客户端幂等键（建议由前端每次提交生成 UUID）；为空时退化为时间窗去重
     * @return 订单 ID；重复提交时返回首次创建的那张订单
     */
    public Long create(OrderCreateRequest request, String idempotencyKey) {
        String key = resolveKey(request, idempotencyKey);
        Duration ttl = StringUtils.hasText(idempotencyKey) ? PENDING_TTL : DUP_WINDOW;

        Boolean acquired = redisService.setIfAbsent(key, PENDING, ttl);
        if (!Boolean.TRUE.equals(acquired)) {
            Long existing = readCompletedOrderId(key);
            if (existing != null) {
                log.info("幂等命中，返回已有订单 orderId={} key={}", existing, key);
                return existing;
            }
            throw new TradeException(ErrorCode.CONFLICT, "订单正在处理中，请勿重复提交");
        }

        try {
            Long orderId = doCreate(request);
            redisService.set(key, DONE_PREFIX + orderId, RESULT_TTL);
            return orderId;
        } catch (RuntimeException e) {
            // 失败必须释放占位，否则用户在 TTL 内连一次重试都发不出来
            redisService.delete(key);
            throw e;
        }
    }

    private Long doCreate(OrderCreateRequest request) {
        Long departureId = request.getDepartureId();
        if (departureId == null) {
            // 无班期（按日期直采）不涉及余位竞争，无需加锁
            return orderService.create(request);
        }
        return distributedLock.execute(String.format(CacheKeyConstants.Lock.DEPARTURE, departureId),
                LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS,
                () -> orderService.create(request));
    }

    private String resolveKey(OrderCreateRequest request, String idempotencyKey) {
        if (StringUtils.hasText(idempotencyKey)) {
            // 带上客户 ID：不同会员即使撞了同一个键也互不影响
            return String.format(CacheKeyConstants.Trade.ORDER_IDEM,
                    request.getCustomerId(), idempotencyKey.trim());
        }
        return String.format(CacheKeyConstants.Trade.ORDER_DUP,
                request.getCustomerId(), request.getRouteId(),
                Objects.toString(request.getDepartureId(), "none"), request.getTravelerCount());
    }

    private Long readCompletedOrderId(String key) {
        Object value = redisService.get(key);
        if (value instanceof String text && text.startsWith(DONE_PREFIX)) {
            try {
                return Long.parseLong(text.substring(DONE_PREFIX.length()));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
