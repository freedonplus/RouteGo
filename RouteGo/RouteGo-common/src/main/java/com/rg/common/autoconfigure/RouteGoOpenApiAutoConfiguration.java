package com.rg.common.autoconfigure;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 统一 OpenAPI 元数据。
 * <p>
 * 每个服务都会得到一份以自身 {@code spring.application.name} 命名的文档，
 * 并统一声明 Bearer JWT 安全方案。网关侧只负责聚合 5 个服务的
 * {@code /v3/api-docs}。
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnProperty(prefix = "routego.openapi", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RouteGoOpenApiAutoConfiguration {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI routeGoOpenAPI(@Value("${spring.application.name:RouteGo}") String applicationName,
                                  @Value("${spring.profiles.active:dev}") String profile) {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName + " API")
                        .version("v1")
                        .description("RouteGo " + applicationName + " (" + profile + ")"))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
