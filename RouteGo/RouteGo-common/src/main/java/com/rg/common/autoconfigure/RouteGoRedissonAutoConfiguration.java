package com.rg.common.autoconfigure;

import com.rg.common.redis.DistributedLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Redisson 分布式锁封装。
 * <p>
 * RedissonClient 由 {@code redisson-spring-boot-starter} 依据
 * {@code spring.data.redis.*} 自动创建，这里只暴露业务侧的锁工具。
 */
@AutoConfiguration(afterName = "org.redisson.spring.starter.RedissonAutoConfiguration")
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(prefix = "routego.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RouteGoRedissonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DistributedLock distributedLock(RedissonClient redissonClient) {
        return new DistributedLock(redissonClient);
    }
}
