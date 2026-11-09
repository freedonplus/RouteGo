package com.rg.trade.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rg.common.redis.DistributedLock;
import com.rg.trade.entity.Orders;
import com.rg.trade.enums.OrderStatus;
import com.rg.trade.mapper.OrdersMapper;
import com.rg.trade.support.OrderStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单超时关单任务（方案书 12 章生产级标准，2026-11-09 审核整改 P0-2）。
 * <p>
 * 待支付订单超过 {@code timeout-minutes}（默认 30 分钟）未支付时自动取消，
 * 取消走 {@link OrderStateMachine}，库存释放、状态流水与主表更新在同一事务内完成，
 * 不会出现「关了单但名额没释放」。
 * <p>
 * 并发与安全设计：
 * <ul>
 *   <li>分布式锁 {@code lock:job:order-timeout-close} 防多实例重复执行；
 *       wait=0，拿不到锁说明别的实例正在跑，本轮直接跳过。</li>
 *   <li>逐单独立事务：一单失败（如刚好被用户支付导致乐观锁冲突）只记 warn，
 *       不影响同批其他订单，下一轮会重扫。</li>
 *   <li>每轮最多处理 {@code scan-limit} 单，防止积压时单次任务跑太久。</li>
 * </ul>
 */
@Component
public class OrderTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutJob.class);

    /** 选主锁，多实例部署时同一时刻只有一个实例执行扫描。 */
    static final String LOCK_KEY = "lock:job:order-timeout-close";
    static final String CANCEL_REASON = "超时未支付，系统自动关单";

    private final OrdersMapper ordersMapper;
    private final OrderStateMachine stateMachine;
    private final DistributedLock distributedLock;

    @Value("${routego.job.order-timeout.enabled:true}")
    private boolean enabled;
    @Value("${routego.job.order-timeout.timeout-minutes:30}")
    private int timeoutMinutes;
    @Value("${routego.job.order-timeout.scan-limit:200}")
    private int scanLimit;
    @Value("${routego.job.order-timeout.lease-seconds:300}")
    private long leaseSeconds;

    public OrderTimeoutJob(OrdersMapper ordersMapper,
                           OrderStateMachine stateMachine,
                           DistributedLock distributedLock) {
        this.ordersMapper = ordersMapper;
        this.stateMachine = stateMachine;
        this.distributedLock = distributedLock;
    }

    /**
     * 每分钟扫描一次，cron 可配置。
     */
    @Scheduled(cron = "${routego.job.order-timeout.cron:0 * * * * ?}")
    public void closeTimeoutOrders() {
        if (!enabled) {
            return;
        }
        boolean acquired;
        try {
            acquired = distributedLock.tryLock(LOCK_KEY, 0, leaseSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("超时关单任务获取分布式锁被中断，本轮跳过");
            return;
        }
        if (!acquired) {
            // 其他实例正在执行，属正常现象，不打日志刷屏
            return;
        }
        try {
            LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
            List<Orders> orders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                    .eq(Orders::getStatus, OrderStatus.UNPAID.getCode())
                    .lt(Orders::getCreateTime, deadline)
                    .orderByAsc(Orders::getCreateTime)
                    .last("LIMIT " + scanLimit));
            if (orders.isEmpty()) {
                return;
            }
            int closed = 0;
            int failed = 0;
            for (Orders order : orders) {
                try {
                    LocalDateTime now = LocalDateTime.now();
                    stateMachine.transition(order, OrderStatus.CANCELED, CANCEL_REASON, update -> {
                        update.setCancelTime(now);
                        update.setCancelReason(CANCEL_REASON);
                    });
                    closed++;
                } catch (Exception e) {
                    failed++;
                    log.warn("超时关单失败 orderId={} msg={}", order.getId(), e.getMessage());
                }
            }
            log.info("超时关单完成 扫描={} 关单={} 失败={}", orders.size(), closed, failed);
        } finally {
            distributedLock.unlock(LOCK_KEY);
        }
    }
}
