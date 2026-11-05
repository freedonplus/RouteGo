package com.rg.common.redis;

import lombok.Data;
import lombok.Getter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

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

    /**
     * key 不存在时写入，返回是否写入成功。用于分布式互斥标记（重发冷却、幂等键）。
     */
    public Boolean setIfAbsent(String key, Object value, Duration ttl) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
    }

    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 原子读取并删除，用于一次性凭证（验证码、幂等键）。
     */
    public <T> T getAndDelete(String key) {
        return (T) redisTemplate.opsForValue().getAndDelete(key);
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

    /**
     * 原子自增并保证 TTL（Lua 脚本）：首次自增或 key 缺失过期时间时补设 TTL，
     * 避免 INCR 与 EXPIRE 分两步执行、进程中断后产生永不过期的计数 key。
     */
    public Long incrementWithTtl(String key, long delta, Duration ttl) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(
                "local c = redis.call('INCRBY', KEYS[1], ARGV[1]) "
                        + "if c == 1 or redis.call('TTL', KEYS[1]) < 0 then "
                        + "redis.call('EXPIRE', KEYS[1], ARGV[2]) end "
                        + "return c",
                Long.class);
        return stringRedisTemplate.execute(script, List.of(key),
                String.valueOf(delta), String.valueOf(ttl.toSeconds()));
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
