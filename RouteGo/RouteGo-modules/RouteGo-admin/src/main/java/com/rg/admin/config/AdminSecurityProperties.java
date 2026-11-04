package com.rg.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * admin 侧安全配置：只有直接对端命中可信代理时，才解析 X-Forwarded-For。
 */
@Data
@Component
@ConfigurationProperties(prefix = "routego.security")
public class AdminSecurityProperties {

    private List<String> trustedProxies = new ArrayList<>();
}
