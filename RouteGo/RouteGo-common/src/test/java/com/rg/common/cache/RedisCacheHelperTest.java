package com.rg.common.cache;

import com.rg.common.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisCacheHelperTest {

    @Mock
    private RedisService redis;
    @Mock
    private RedissonClient redisson;
    @Mock
    private RLock lock;

    private RedisCacheHelper helper;

    @BeforeEach
    void setUp() {
        helper = new RedisCacheHelper(redis, redisson);
    }

    @Test
    void returnsCachedValueWithoutLoading() {
        when(redis.get("k")).thenReturn("v");
        assertThat(helper.getOrLoad("k", Duration.ofMinutes(1), () -> "db")).isEqualTo("v");
        verifyNoInteractions(redisson);
    }

    @Test
    void nullSentinelIsReturnedAsNullWithoutLoading() {
        when(redis.get("k")).thenReturn(CacheNullValue.MARKER);
        AtomicInteger calls = new AtomicInteger();
        assertThat(helper.getOrLoad("k", Duration.ofMinutes(1), () -> {
            calls.incrementAndGet();
            return "db";
        })).isNull();
        assertThat(calls.get()).isZero();
        verifyNoInteractions(redisson);
    }

    @Test
    void legacySentinelObjectStillRecognized() {
        when(redis.get("k")).thenReturn(CacheNullValue.INSTANCE);
        assertThat(helper.getOrLoad("k", Duration.ofMinutes(1), () -> "db")).isNull();
        verifyNoInteractions(redisson);
    }

    @Test
    void cachesNullFor60Seconds() throws Exception {
        when(redis.get("k")).thenReturn(null);
        when(redisson.getLock("lock:cache:rebuild:k")).thenReturn(lock);
        when(lock.tryLock(3, 10, TimeUnit.SECONDS)).thenReturn(true);

        String loaded = helper.getOrLoad("k", Duration.ofMinutes(1), () -> null);
        assertThat(loaded).isNull();

        verify(redis).set("k", CacheNullValue.MARKER, Duration.ofSeconds(60));
        verify(lock).unlock();
    }

    @Test
    void loadsAndCachesMissWithJitteredTtl() throws Exception {
        when(redis.get("k")).thenReturn(null);
        when(redisson.getLock("lock:cache:rebuild:k")).thenReturn(lock);
        when(lock.tryLock(3, 10, TimeUnit.SECONDS)).thenReturn(true);

        assertThat(helper.getOrLoad("k", Duration.ofMinutes(1), () -> "db")).isEqualTo("db");

        verify(redis).set(eq("k"), eq("db"), any(Duration.class));
        verify(lock).unlock();
    }

    @Test
    void ttlJitterStaysWithinTenPercent() {
        for (int i = 0; i < 100; i++) {
            assertThat(helper.jitter(Duration.ofSeconds(1000)))
                    .isBetween(Duration.ofSeconds(900), Duration.ofSeconds(1100));
        }
    }

    @Test
    void evictAfterCommitWithoutTransactionDeletesImmediately() {
        helper.evictAfterCommit("a", "b");
        verify(redis).delete(ArgumentMatchers.<String>anyCollection());
    }

    @Test
    void evictSingleKeyUsesSingleKeyDelete() {
        helper.evict("a");
        verify(redis).delete("a");
        verify(redis, never()).delete(ArgumentMatchers.<String>anyCollection());
    }
}
