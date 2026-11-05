package com.rg.common.autoconfigure;

import com.rg.common.crypto.CryptoProperties;
import com.rg.common.crypto.SensitiveCipher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 敏感字段加密装配。
 */
@AutoConfiguration
@EnableConfigurationProperties(CryptoProperties.class)
public class RouteGoCryptoAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SensitiveCipher.class)
    public SensitiveCipher sensitiveCipher(CryptoProperties properties) {
        return new SensitiveCipher(properties);
    }
}
