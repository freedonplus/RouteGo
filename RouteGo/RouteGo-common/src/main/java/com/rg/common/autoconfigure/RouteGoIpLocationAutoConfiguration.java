package com.rg.common.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rg.common.ip.ApihzIpLocationClient;
import com.rg.common.ip.IpLocationClient;
import com.rg.common.ip.IpLocationProperties;
import com.rg.common.ip.IpLocationService;
import com.rg.common.redis.RedisService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * IP 归属地装配。配置关闭或未配置密钥时，查询服务返回 {@code null}，
 * 登录日志只记录原始 IP，不影响主流程。
 */
@AutoConfiguration
@EnableConfigurationProperties(IpLocationProperties.class)
public class RouteGoIpLocationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "routego.ip-location", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public IpLocationClient ipLocationClient(IpLocationProperties properties, ObjectMapper objectMapper) {
        return new ApihzIpLocationClient(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public IpLocationService ipLocationService(ObjectProvider<IpLocationClient> clientProvider,
                                               RedisService redisService,
                                               IpLocationProperties properties) {
        return new IpLocationService(clientProvider.getIfAvailable(), redisService, properties);
    }
}
