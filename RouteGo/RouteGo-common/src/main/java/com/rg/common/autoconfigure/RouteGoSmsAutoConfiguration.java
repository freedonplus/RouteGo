package com.rg.common.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rg.common.sms.ApihzSmsClient;
import com.rg.common.sms.SmsClient;
import com.rg.common.sms.SmsProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 短信配置装配。
 * <p>
 * {@link SmsProperties} 始终注册（关闭短信时业务层仍需要读取验证码有效期等参数），
 * 只有 {@code routego.sms.enabled=true} 时才创建真正的发送客户端。
 */
@AutoConfiguration
@EnableConfigurationProperties(SmsProperties.class)
public class RouteGoSmsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SmsClient.class)
    @ConditionalOnProperty(prefix = "routego.sms", name = "enabled", havingValue = "true")
    public SmsClient smsClient(SmsProperties properties, ObjectMapper objectMapper) {
        return new ApihzSmsClient(properties, objectMapper);
    }
}
