package com.rg.common.ip;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * IP 归属地查询配置（接口盒子 apihz）。
 */
@ConfigurationProperties(prefix = "routego.ip-location")
public class IpLocationProperties {

    /** 是否启用归属地查询。关闭后登录日志只记录 IP。 */
    private boolean enabled = true;

    private String endpoint = "https://cn.apihz.cn/api/ip/chaapi.php";

    /** 开发者 ID，与短信共用同一套账号。 */
    private String id;

    /** 开发者 KEY。 */
    private String key;

    /** 查询通道 0-6，0 为官方库（速度最快）。 */
    private String td = "0";

    private Duration timeout = Duration.ofSeconds(3);

    /** 结果缓存时长，避免同一 IP 反复消耗接口频次。 */
    private Duration cacheTtl = Duration.ofDays(7);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTd() {
        return td;
    }

    public void setTd(String td) {
        this.td = td;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }
}
