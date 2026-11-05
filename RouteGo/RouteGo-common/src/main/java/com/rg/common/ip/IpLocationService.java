package com.rg.common.ip;

import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP 归属地查询服务：本地内网判定 → Redis 缓存 → 第三方接口。
 * <p>
 * 任何失败都返回 {@code null}，绝不抛出异常——归属地只是展示信息，
 * 不能因为它把登录或日志流程带崩。
 */
public class IpLocationService {

    private static final Logger log = LoggerFactory.getLogger(IpLocationService.class);

    private static final String LOOPBACK = "内网 IP";

    private final IpLocationClient client;
    private final RedisService redisService;
    private final IpLocationProperties properties;

    public IpLocationService(IpLocationClient client,
                             RedisService redisService,
                             IpLocationProperties properties) {
        this.client = client;
        this.redisService = redisService;
        this.properties = properties;
    }

    /**
     * 查询 IP 归属地，内网地址直接返回「内网 IP」且不打接口。
     */
    public IpLocation lookup(String ip) {
        if (!StringUtils.hasText(ip)) {
            return null;
        }
        if (isPrivate(ip)) {
            return new IpLocation(null, null, null, null, null, LOOPBACK);
        }
        String cacheKey = String.format(CacheKeyConstants.Admin.IP_LOCATION, ip);
        try {
            IpLocation cached = redisService.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (RuntimeException e) {
            // 反序列化失败（历史数据）直接忽略，回源查询
            redisService.delete(cacheKey);
        }

        IpLocation location = client == null ? null : client.lookup(ip);
        if (location != null) {
            try {
                redisService.set(cacheKey, location, properties.getCacheTtl());
            } catch (RuntimeException e) {
                log.debug("IP 归属地缓存写入失败: {}", e.getMessage());
            }
        }
        return location;
    }

    /**
     * 展示用短文本，查不到时回退空串。
     */
    public String describe(String ip) {
        IpLocation location = lookup(ip);
        return location == null ? "" : location.shortText();
    }

    static boolean isPrivate(String ip) {
        if (ip == null) {
            return false;
        }
        String value = ip.trim();
        if (value.startsWith("10.") || value.startsWith("192.168.")
                || value.startsWith("127.") || value.startsWith("169.254.")) {
            return true;
        }
        if (value.startsWith("172.")) {
            int dot = value.indexOf('.', 4);
            if (dot > 4) {
                try {
                    int second = Integer.parseInt(value.substring(4, dot));
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
            return false;
        }
        if (value.equals("::1") || value.startsWith("fc") || value.startsWith("fd")
                || value.startsWith("fe80:")) {
            return true;
        }
        // 非法 IP（如 "unknown"）也按不可查询处理，避免浪费接口额度
        try {
            InetAddress address = InetAddress.getByName(value);
            return address.isSiteLocalAddress() || address.isLoopbackAddress()
                    || address.isLinkLocalAddress();
        } catch (UnknownHostException e) {
            return true;
        }
    }
}
