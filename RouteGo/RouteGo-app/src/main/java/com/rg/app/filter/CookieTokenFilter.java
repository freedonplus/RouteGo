package com.rg.app.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 将 HttpOnly Cookie 中的裸 Token 包装成 {@code Authorization: Bearer <token>}，
 * 让 Sa-Token 同时支持 Cookie 和 Bearer 两种读取方式。
 * <p>
 * 背景：Sa-Token 的 Cookie 写入的是裸 Token，而 token-prefix=Bearer，
 * 直接读 Cookie 会因前缀不匹配而失败。这里只在没有 Authorization 头时
 * 从 Cookie 补一个带头的前缀，已有 Bearer 的请求完全不动。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class CookieTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION = "Authorization";

    @Value("${sa-token.token-name:Authorization}")
    private String cookieName;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(AUTHORIZATION);
        if (StringUtils.hasText(authorization)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = readTokenCookie(request);
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerValue = token.startsWith(BEARER_PREFIX) ? token : BEARER_PREFIX + token;
        filterChain.doFilter(new AuthorizationHeaderRequestWrapper(request, headerValue), response);
    }

    private String readTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue().trim();
            }
        }
        return null;
    }

    private static final class AuthorizationHeaderRequestWrapper extends HttpServletRequestWrapper {

        private final String authorizationValue;

        private AuthorizationHeaderRequestWrapper(HttpServletRequest request, String authorizationValue) {
            super(request);
            this.authorizationValue = authorizationValue;
        }

        @Override
        public String getHeader(String name) {
            if (AUTHORIZATION.equalsIgnoreCase(name)) {
                return authorizationValue;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (AUTHORIZATION.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(authorizationValue));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            Enumeration<String> originals = super.getHeaderNames();
            if (originals != null) {
                while (originals.hasMoreElements()) {
                    names.add(originals.nextElement());
                }
            }
            names.add(AUTHORIZATION);
            return Collections.enumeration(names);
        }
    }
}
