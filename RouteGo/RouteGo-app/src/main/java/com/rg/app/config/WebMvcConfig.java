package com.rg.app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 单体应用的全局跨域配置。
 * <p>
 * Cookie 鉴权必须配合明确的前端 Origin 和 allowCredentials，不能再用 *。
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final SecurityProperties securityProperties;

    public WebMvcConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(securityProperties.getAllowedOrigins().toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-User-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
