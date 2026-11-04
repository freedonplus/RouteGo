package com.rg.common.autoconfigure;

import com.rg.common.cache.CacheHelper;
import com.rg.common.cache.RedisCacheHelper;
import com.rg.common.redis.RedisService;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 注册通用缓存模板。
 */
@AutoConfiguration(after = {RouteGoRedisAutoConfiguration.class, RouteGoRedissonAutoConfiguration.class})
@ConditionalOnBean({RedisService.class, RedissonClient.class})
public class RouteGoCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CacheHelper cacheHelper(RedisService redisService, RedissonClient redissonClient) {
        return new RedisCacheHelper(redisService, redissonClient);
    }
}
