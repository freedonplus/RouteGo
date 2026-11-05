package com.rg.common.crypto;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 敏感字段加解密与盲索引工具。
 * <p>
 * 加密采用 AES/GCM/NoPadding，每次加密随机生成 12 字节 IV 并前置到密文里，
 * 输出 {@code Base64(IV || CipherText || Tag)}；同一个明文每次加密结果都不同，
 * 因此无法直接按密文做等值查询——需要等值查询的列（手机号）额外保存
 * {@link #blindIndex(String)} 的结果。
 * <p>
 * 解密对历史明文数据是容错的：解析失败时原样返回，方便老数据平滑过渡。
 */
public class SensitiveCipher {

    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final boolean enabled;
    private final SecretKeySpec aesKey;
    private final SecretKeySpec hmacKey;
    private final SecureRandom random = new SecureRandom();

    public SensitiveCipher(CryptoProperties properties) {
        this.enabled = properties.isEnabled();
        if (!enabled) {
            this.aesKey = null;
            this.hmacKey = null;
            return;
        }
        String aes = properties.getAesKey();
        String hmac = properties.getHmacKey();
        if (aes == null || aes.isBlank() || hmac == null || hmac.isBlank()) {
            throw new IllegalStateException(
                    "启用敏感字段加密时必须配置 routego.crypto.aes-key 与 routego.crypto.hmac-key");
        }
        byte[] aesBytes = Base64.getDecoder().decode(aes.trim());
        int len = aesBytes.length;
        if (len != 16 && len != 24 && len != 32) {
            throw new IllegalStateException("routego.crypto.aes-key 解码后必须是 16/24/32 字节，当前为 " + len + " 字节");
        }
        this.aesKey = new SecretKeySpec(aesBytes, "AES");
        this.hmacKey = new SecretKeySpec(hmac.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 加密。空值直接返回 null；已关闭加密时原样返回。
     */
    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty() || !enabled) {
            return plain;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("敏感字段加密失败", e);
        }
    }

    /**
     * 解密。空值返回 null；密文不合法（例如历史明文）时原样返回。
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty() || !enabled) {
            return cipherText;
        }
        try {
            byte[] all = Base64.getDecoder().decode(cipherText);
            if (all.length <= GCM_IV_LENGTH) {
                return cipherText;
            }
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, aesKey,
                    new GCMParameterSpec(GCM_TAG_BITS, all, 0, GCM_IV_LENGTH));
            byte[] plain = cipher.doFinal(all, GCM_IV_LENGTH, all.length - GCM_IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 历史明文或密钥变更后的旧密文：不抛错，交给上层按原文处理
            return cipherText;
        }
    }

    /**
     * 盲索引：HMAC-SHA256(值) 的小写十六进制，用于密文列的等值查询。
     */
    public String blindIndex(String plain) {
        if (plain == null || plain.isEmpty()) {
            return null;
        }
        if (!enabled) {
            return plain;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);
            return HexFormat.of().formatHex(mac.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("盲索引计算失败", e);
        }
    }

    /**
     * 手机号脱敏：138****8000。号码过短时按长度截断处理。
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return null;
        }
        String value = decrypt(phone);
        if (value == null || value.length() < 7) {
            return "***";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    /**
     * 证件号脱敏：保留前 4 位与后 4 位。
     */
    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.isEmpty()) {
            return null;
        }
        String value = decrypt(idCard);
        if (value == null || value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "**********" + value.substring(value.length() - 4);
    }
}
