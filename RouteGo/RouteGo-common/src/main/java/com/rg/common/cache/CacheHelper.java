package com.rg.common.cache;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 缓存读写模板。
 * <p>
 * 统一处理缓存穿透（空值缓存）、击穿（互斥锁重建）与雪崩（TTL 抖动），
 * 业务代码只需要提供 key、TTL 和回源逻辑。
 */
public interface CacheHelper {

    /**
     * 读缓存，未命中时回源并写入缓存；回源返回 {@code null} 时写入 60 秒空值哨兵。
     */
    <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader);

    /**
     * 立即删除缓存。
     */
    void evict(String... keys);

    /**
     * 事务提交后删除缓存；不在事务中时立即删除。
     */
    void evictAfterCommit(String... keys);

    /**
     * 事务提交后对计数器自增；不在事务中时立即自增。
     * <p>
     * 用于「版本号失效」模式：把版本号自增而不是删除，旧版本 key 自然不可达，
     * 避免删除后版本号回退到初始值、重新命中旧缓存。
     */
    void incrementAfterCommit(String key, long delta);
}
