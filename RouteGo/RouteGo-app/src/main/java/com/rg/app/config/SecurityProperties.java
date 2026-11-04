package com.rg.app.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 安全相关配置。
 * <p>
 * 通过 {@code @EnableConfigurationProperties} 注册并绑定
 * {@code routego.security.*}，启动时打印实际生效的允许来源。
 */
@Slf4j
@Data
@ConfigurationProperties(prefix = "routego.security")
public class SecurityProperties {

    private List<String> allowedOrigins = new ArrayList<>(Arrays.asList(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:3000"));

    @PostConstruct
    void logAllowedOrigins() {
        log.info("routego.security.allowed-origins = {}", allowedOrigins);
    }
}
