package com.rg.common.autoconfigure;

import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 服务启动完成后打印 SpringDoc 访问地址，方便本地联调时直接点开。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnProperty(prefix = "routego.openapi", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RouteGoSpringDocAutoConfiguration {

    @Bean
    public SpringDocAddressLogger springDocAddressLogger(Environment environment) {
        return new SpringDocAddressLogger(environment);
    }

    public static class SpringDocAddressLogger implements ApplicationListener<ApplicationReadyEvent> {

        private static final Logger log = LoggerFactory.getLogger(SpringDocAddressLogger.class);

        private final Environment environment;

        public SpringDocAddressLogger(Environment environment) {
            this.environment = environment;
        }

        @Override
        public void onApplicationEvent(ApplicationReadyEvent event) {
            String applicationName = environment.getProperty("spring.application.name", "RouteGo");
            String port = environment.getProperty("local.server.port",
                    environment.getProperty("server.port", "8080"));
            String contextPath = normalizePath(environment.getProperty("server.servlet.context-path", ""));

            String swaggerPath = normalizePath(environment.getProperty("springdoc.swagger-ui.path", "/swagger-ui.html"));
            String apiDocsPath = normalizePath(environment.getProperty("springdoc.api-docs.path", "/v3/api-docs"));
            String profiles = String.join(",", environment.getActiveProfiles());
            if (profiles.isBlank()) {
                profiles = "default";
            }

            log.info("==============================================================");
            log.info("[{}] profile       : {}", applicationName, profiles);
            log.info("[{}] SpringDoc UI  : http://localhost:{}{}{}", applicationName, port, contextPath, swaggerPath);
            log.info("[{}] OpenAPI JSON : http://localhost:{}{}{}", applicationName, port, contextPath, apiDocsPath);
            log.info("==============================================================");
        }

        private String normalizePath(String path) {
            if (path == null || path.isBlank()) {
                return "";
            }
            String normalized = path.trim();
            if (!normalized.startsWith("/")) {
                normalized = "/" + normalized;
            }
            if (normalized.endsWith("/") && normalized.length() > 1) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            return normalized;
        }
    }
}
