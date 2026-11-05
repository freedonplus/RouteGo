package com.rg.admin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 第三方登录配置注册。
 */
@Configuration
@EnableConfigurationProperties(OauthProperties.class)
public class OauthConfiguration {
}
