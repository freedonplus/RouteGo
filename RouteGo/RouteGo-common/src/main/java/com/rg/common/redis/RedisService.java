package com.rg.common.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 常用 Redis 操作封装。
 * <p>
 * 对象值走 JSON 序列化（见 {@code RouteGoRedisAutoConfiguration}），
 * 计数器统一使用 {@link StringRedisTemplate}，保证 Redis 的 INCR 语义。
 */
@SuppressWarnings("unchecked")
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisService(RedisTemplate<String, Object> redisTemplate,
                        StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public void setIfAbsent(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
    }

    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Boolean expire(String key, Duration ttl) {
        return redisTemplate.expire(key, ttl);
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    /** 原子自增，返回自增后的值 */
    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }

    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public <T> T hGet(String key, String field) {
        return (T) redisTemplate.opsForHash().get(key, field);
    }

    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public Long hDelete(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }

    public Long lPush(String key, Object value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    public <T> List<T> lRange(String key, long start, long end) {
        return (List<T>) redisTemplate.opsForList().range(key, start, end);
    }
}
