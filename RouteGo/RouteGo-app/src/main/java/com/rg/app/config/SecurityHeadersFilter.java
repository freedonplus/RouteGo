package com.rg.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 统一安全响应头。
 * <p>
 * 只作用于本项目自身的响应（HTML/JSON/文件），不替代 Nginx 上的 TLS 配置。
 * 几点取舍：
 * <ul>
 *   <li>{@code X-Frame-Options: DENY}：后台与 App 都不需要被 iframe 嵌套，直接禁掉点击劫持；</li>
 *   <li>{@code CSP default-src 'none'}：接口返回的是 JSON 和二进制，本就不该加载任何子资源。
 *       Swagger UI 是真正的 HTML 页面，会被这条策略打断，因此单独放行；</li>
 *   <li>{@code HSTS} 只在请求确实是 HTTPS 时下发，避免本地 http 调试被浏览器记住；</li>
 *   <li>不设 {@code Cross-Origin-Resource-Policy}，避免影响跨端口调试的图片加载。</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String CSP_API = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'";
    private static final String HSTS = "max-age=31536000; includeSubDomains";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        if (!isApiDocumentation(request.getRequestURI())) {
            response.setHeader("Content-Security-Policy", CSP_API);
        }
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", HSTS);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Swagger UI 与 OpenAPI 文档需要自己加载脚本与样式，跳过 CSP。
     */
    private boolean isApiDocumentation(String uri) {
        return uri != null && (uri.contains("/swagger-ui") || uri.contains("/v3/api-docs"));
    }
}
