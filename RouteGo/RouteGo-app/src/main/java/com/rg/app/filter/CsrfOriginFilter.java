package com.rg.app.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rg.app.config.SecurityProperties;
import com.rg.common.util.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 轻量 CSRF 防护：对写操作校验浏览器 Origin。
 * <p>
 * Cookie 会被浏览器自动携带，所以 POST/PUT/PATCH/DELETE 必须限制来源。
 * 没有 Origin 的请求（curl、服务端调用）放行；带 Origin 的请求必须在
 * {@code routego.security.allowed-origins} 白名单内。
 * <p>
 * 这是 SameSite=Lax 之外的补充，不替代正式的 CSRF Token。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class CsrfOriginFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    public CsrfOriginFilter(SecurityProperties securityProperties, ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SAFE_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = request.getHeader("Origin");
        if (!StringUtils.hasText(origin)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean allowed = securityProperties.getAllowedOrigins().stream()
                .anyMatch(item -> item.equalsIgnoreCase(origin));
        if (allowed) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), Result.error(403, "非法跨站请求"));
    }
}
