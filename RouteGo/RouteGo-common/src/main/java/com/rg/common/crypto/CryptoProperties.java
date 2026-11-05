package com.rg.common.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 敏感字段加密配置。
 * <p>
 * 手机号、证件号等个人敏感信息在库里只保存密文，另存一列 HMAC 盲索引用于等值查询。
 * 密钥必须通过配置文件或环境变量注入，生产环境禁止使用仓库里的默认值。
 */
@ConfigurationProperties(prefix = "routego.crypto")
public class CryptoProperties {

    /** 是否启用加密。关闭时按明文读写，仅建议本地临时调试使用。 */
    private boolean enabled = true;

    /**
     * AES 密钥（Base64），解码后必须是 16 / 24 / 32 字节，推荐 32 字节。
     */
    private String aesKey;

    /**
     * 盲索引专用的 HMAC 密钥。必须与 AES 密钥不同：
     * 盲索引会暴露「两个值是否相同」，密钥泄露的后果比 AES 更严重。
     */
    private String hmacKey;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public String getHmacKey() {
        return hmacKey;
    }

    public void setHmacKey(String hmacKey) {
        this.hmacKey = hmacKey;
    }
}
