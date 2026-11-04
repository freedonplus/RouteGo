package com.rg.common.autoconfigure;

import com.rg.common.cache.CacheHelper;
import com.rg.common.cache.RedisCacheHelper;
import com.rg.common.redis.RedisService;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 注册通用缓存模板。
 * <p>
 * 不能用 {@code @ConditionalOnBean}：条件在解析阶段求值，此时 Redisson 的
 * 自动配置还没注册 RedissonClient，会被误判为缺失。
 */
@AutoConfiguration(after = RouteGoRedisAutoConfiguration.class,
        afterName = "org.redisson.spring.starter.RedissonAutoConfiguration")
@ConditionalOnClass({RedisService.class, RedissonClient.class})
public class RouteGoCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CacheHelper cacheHelper(RedisService redisService, RedissonClient redissonClient) {
        return new RedisCacheHelper(redisService, redissonClient);
    }
}
