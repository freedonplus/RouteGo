package com.rg.common.cache;

import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.redis.RedisService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redis + Redisson 的缓存模板。
 */
public class RedisCacheHelper implements CacheHelper {

    private static final Duration NULL_TTL = Duration.ofSeconds(60);
    private static final Duration LOCK_WAIT = Duration.ofSeconds(3);
    private static final Duration LOCK_LEASE = Duration.ofSeconds(10);
    private static final Duration RECHECK_WINDOW = Duration.ofMillis(500);
    private static final long RECHECK_INTERVAL_MILLIS = 50;

    private final RedisService redisService;
    private final RedissonClient redissonClient;

    public RedisCacheHelper(RedisService redisService, RedissonClient redissonClient) {
        this.redisService = redisService;
        this.redissonClient = redissonClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader) {
        Object cached = redisService.get(key);
        if (CacheNullValue.isNullMarker(cached)) {
            return null;
        }
        if (cached != null) {
            return (T) cached;
        }

        RLock lock = redissonClient.getLock(String.format(CacheKeyConstants.Lock.CACHE_REBUILD, key));
        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_WAIT.toSeconds(), LOCK_LEASE.toSeconds(), TimeUnit.SECONDS);
            if (!locked) {
                T rechecked = readWithinWindow(key);
                if (rechecked != null) {
                    return rechecked;
                }
                T value = loader.get();
                if (value != null) {
                    // 兜底查库后回写缓存，避免击穿窗口内每次请求都直打 DB；
                    // 用 setIfAbsent 降低与持锁方的覆盖竞争
                    redisService.setIfAbsent(key, value, jitter(ttl));
                }
                return value;
            }

            cached = redisService.get(key);
            if (CacheNullValue.isNullMarker(cached)) {
                return null;
            }
            if (cached != null) {
                return (T) cached;
            }

            T value = loader.get();
            if (value == null) {
                redisService.set(key, CacheNullValue.MARKER, NULL_TTL);
            } else {
                redisService.set(key, value, jitter(ttl));
            }
            return value;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return loader.get();
        } finally {
            if (locked) {
                try {
                    lock.unlock();
                } catch (IllegalMonitorStateException ignored) {
                    // 锁已过期自动释放
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T readWithinWindow(String key) {
        long deadline = System.nanoTime() + RECHECK_WINDOW.toNanos();
        while (System.nanoTime() < deadline) {
            try {
                Thread.sleep(RECHECK_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            Object value = redisService.get(key);
            if (CacheNullValue.isNullMarker(value)) {
                return null;
            }
            if (value != null) {
                return (T) value;
            }
        }
        return null;
    }

    @Override
    public void evict(String... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        if (keys.length == 1) {
            redisService.delete(keys[0]);
        } else {
            redisService.delete(List.of(keys));
        }
    }

    @Override
    public void evictAfterCommit(String... keys) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict(keys);
                }
            });
        } else {
            evict(keys);
        }
    }

    @Override
    public void incrementAfterCommit(String key, long delta) {
        Runnable action = () -> redisService.increment(key, delta);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    /**
     * 给 TTL 加 ±10% 随机抖动，避免同一时刻批量过期。
     */
    Duration jitter(Duration ttl) {
        double factor = 0.9 + ThreadLocalRandom.current().nextDouble() * 0.2;
        return Duration.ofNanos((long) (ttl.toNanos() * factor));
    }
}
