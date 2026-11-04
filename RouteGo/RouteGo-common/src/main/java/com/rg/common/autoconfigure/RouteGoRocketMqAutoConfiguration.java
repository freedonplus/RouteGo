package com.rg.common.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rg.common.mq.EventPublisher;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * RocketMQ 事件发布能力。
 * <p>
 * 只有配置了 {@code rocketmq.name-server} 且 RocketMQ starter 成功创建
 * {@link RocketMQTemplate} 时才生效；没部署 RocketMQ 的环境不会因此启动失败。
 */
@AutoConfiguration(afterName = "org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration")
@ConditionalOnClass({RocketMQTemplate.class, RocketMQListener.class})
@ConditionalOnBean(RocketMQTemplate.class)
@ConditionalOnProperty(prefix = "routego.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RouteGoRocketMqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EventPublisher eventPublisher(RocketMQTemplate rocketMQTemplate, ObjectMapper objectMapper) {
        return new EventPublisher(rocketMQTemplate, objectMapper);
    }
}
