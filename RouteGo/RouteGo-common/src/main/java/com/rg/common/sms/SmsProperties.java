package com.rg.common.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 短信服务配置。
 * <p>
 * 默认对接「接口盒子 apihz」的短信代发送接口，密钥只允许写在配置文件或环境变量里，
 * 不要硬编码到代码中。
 */
@ConfigurationProperties(prefix = "routego.sms")
public class SmsProperties {

    /** 是否启用短信发送能力。关闭时发送接口直接返回「短信服务未启用」。 */
    private boolean enabled = false;

    /** 接口地址，默认 apihz 短信代发送。 */
    private String endpoint = "https://cn.apihz.cn/api/sms/dfapi.php";

    /** 开发者 ID。 */
    private String id;

    /** 开发者 KEY。 */
    private String key;

    /** 动态秘钥（开启了动态秘钥校验时必填）。 */
    private String dkey;

    /** 连接与读取超时。 */
    private Duration timeout = Duration.ofSeconds(5);

    /** 验证码长度。 */
    private int codeLength = 6;

    /** 验证码有效期。 */
    private Duration codeTtl = Duration.ofMinutes(5);

    /** 同一接收方的重发间隔。 */
    private Duration resendInterval = Duration.ofSeconds(60);

    /** 同一接收方每日最多发送条数（apihz 单号码限制 1 天 20 条）。 */
    private int dailyLimit = 20;

    /** 同一 IP 每小时最多发送条数，防止短信轰炸。 */
    private int hourlyLimitPerIp = 20;

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

    public String getDkey() {
        return dkey;
    }

    public void setDkey(String dkey) {
        this.dkey = dkey;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    public Duration getCodeTtl() {
        return codeTtl;
    }

    public void setCodeTtl(Duration codeTtl) {
        this.codeTtl = codeTtl;
    }

    public Duration getResendInterval() {
        return resendInterval;
    }

    public void setResendInterval(Duration resendInterval) {
        this.resendInterval = resendInterval;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public int getHourlyLimitPerIp() {
        return hourlyLimitPerIp;
    }

    public void setHourlyLimitPerIp(int hourlyLimitPerIp) {
        this.hourlyLimitPerIp = hourlyLimitPerIp;
    }
}
