package com.rg.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * 客户端 IP 解析。
 * <p>
 * 默认只信任 {@code getRemoteAddr()}；只有当直接对端命中 {@code trusted-proxies}
 * （支持精确 IP、CIDR 网段、{@code *} 通配）时，才从 {@code X-Forwarded-For}
 * 从右往左取第一个非可信地址，避免客户端伪造。{@code X-Forwarded-For} 缺失时
 * 回退到 {@code X-Real-IP}。
 */
public final class IpUtils {

    private static final String UNKNOWN = "unknown";

    private IpUtils() {
    }

    public static String resolveClientIp(HttpServletRequest request, List<String> trustedProxies) {
        if (request == null) {
            return null;
        }
        String remoteAddr = normalize(request.getRemoteAddr());
        if (trustedProxies == null || trustedProxies.isEmpty()) {
            return remoteAddr;
        }
        if (!isTrusted(remoteAddr, trustedProxies)) {
            return remoteAddr;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String[] hops = forwarded.split(",");
            for (int i = hops.length - 1; i >= 0; i--) {
                String hop = normalize(hops[i]);
                if (!StringUtils.hasText(hop) || UNKNOWN.equalsIgnoreCase(hop)) {
                    continue;
                }
                if (!isTrusted(hop, trustedProxies)) {
                    return hop;
                }
            }
        }

        String realIp = normalize(request.getHeader("X-Real-IP"));
        if (StringUtils.hasText(realIp) && !UNKNOWN.equalsIgnoreCase(realIp)) {
            return realIp;
        }
        return remoteAddr;
    }

    /**
     * 判断 IP 是否命中可信代理规则。规则支持：
     * <ul>
     *   <li>{@code *}：信任全部（仅建议在可控的内网网关后使用）</li>
     *   <li>{@code 10.0.0.0/8}：CIDR 网段</li>
     *   <li>{@code 127.0.0.1} / {@code ::1}：精确匹配</li>
     * </ul>
     */
    public static boolean isTrusted(String ip, List<String> rules) {
        if (!StringUtils.hasText(ip) || rules == null) {
            return false;
        }
        for (String rule : rules) {
            if (!StringUtils.hasText(rule)) {
                continue;
            }
            String trimmed = rule.trim();
            if ("*".equals(trimmed)) {
                return true;
            }
            if (trimmed.contains("/")) {
                if (inCidr(ip, trimmed)) {
                    return true;
                }
            } else if (trimmed.equalsIgnoreCase(ip)) {
                return true;
            }
        }
        return false;
    }

    static boolean inCidr(String ip, String cidr) {
        int slash = cidr.indexOf('/');
        if (slash <= 0) {
            return false;
        }
        String network = cidr.substring(0, slash);
        int prefix;
        try {
            prefix = Integer.parseInt(cidr.substring(slash + 1));
        } catch (NumberFormatException e) {
            return false;
        }
        try {
            byte[] ipBytes = InetAddress.getByName(ip).getAddress();
            byte[] netBytes = InetAddress.getByName(network).getAddress();
            if (ipBytes.length != netBytes.length || prefix < 0 || prefix > ipBytes.length * 8) {
                return false;
            }
            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (ipBytes[i] != netBytes[i]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits);
            return (ipBytes[fullBytes] & mask) == (netBytes[fullBytes] & mask);
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private static String normalize(String ip) {
        if (!StringUtils.hasText(ip)) {
            return null;
        }
        String value = ip.trim();
        // IPv6 回环与 IPv4 映射地址统一成 IPv4，便于和可信列表精确匹配
        if ("0:0:0:0:0:0:0:1".equals(value) || "::1".equals(value)) {
            return "127.0.0.1";
        }
        if (value.startsWith("::ffff:")) {
            return value.substring(7);
        }
        return value;
    }
}
