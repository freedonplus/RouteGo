package com.rg.common.redis;

import com.rg.common.exception.BusinessException;
import com.rg.common.exception.ErrorCode;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 分布式锁封装。
 * <p>
 * 使用示例：
 * <pre>{@code
 * distributedLock.execute("lock:order:" + orderId, 3, 10, TimeUnit.SECONDS,
 *         () -> orderService.create(orderId));
 * }</pre>
 * 业务代码不需要关心锁的释放与中断恢复。
 */
public class DistributedLock {

    private final RedissonClient redissonClient;

    public DistributedLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 加锁执行并返回结果。拿不到锁时抛出 409，避免请求无限堆积。
     */
    public <T> T execute(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> action) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, unit);
            if (!acquired) {
                throw new BusinessException(ErrorCode.CONFLICT, "操作正在处理中，请稍后重试");
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "获取分布式锁被中断");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 加锁执行无返回值任务。
     */
    public void execute(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Runnable action) {
        execute(lockKey, waitTime, leaseTime, unit, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 只尝试加锁，由调用方负责释放。
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        return redissonClient.getLock(lockKey).tryLock(waitTime, leaseTime, unit);
    }

    /**
     * 释放当前线程持有的锁。
     */
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
