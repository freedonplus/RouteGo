package com.rg.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 客户端 IP 解析。
 * <p>
 * 默认只信任 {@code getRemoteAddr()}；只有当直接对端在配置的可信代理列表中时，
 * 才从 {@code X-Forwarded-For} 从右往左取第一个非可信地址，避免客户端伪造。
 */
public final class IpUtils {

    private IpUtils() {
    }

    public static String resolveClientIp(HttpServletRequest request, List<String> trustedProxies) {
        if (request == null) {
            return null;
        }
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxies == null || trustedProxies.isEmpty() || !trustedProxies.contains(remoteAddr)) {
            return remoteAddr;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(forwarded)) {
            return remoteAddr;
        }
        String[] hops = forwarded.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            if (hop.isEmpty()) {
                continue;
            }
            if (!trustedProxies.contains(hop)) {
                return hop;
            }
        }
        return hops[0].trim();
    }
}
