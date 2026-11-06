package com.rg.common.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 文件临时预览链接的签名与解析。
 * <p>
 * 图片通过 {@code <img>} 标签加载时无法携带 Authorization 头（也没有跨域 Cookie），
 * 直接引用 {@code /admin/system/file/{id}/download} 会拿到 401。这里签发带过期时间的
 * HMAC-SHA256 签名链接（默认 {@code /app/file/preview?fid=&expires=&sign=}），
 * 由公开端点校验签名后回源文件内容。
 * <p>
 * 任何响应里的文件地址都可以用 {@link #extractFileId(String)} 还原文件 ID——
 * 兼容数据库存的下载地址、前端补了 {@code /api} 前缀的地址、以及本类签发的临时链接，
 * 保证「读出来换临时链接、写回去还原标准地址」形成闭环，签名 URL 不会落库。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "routego.storage.temp-url")
public class TempUrlSigner {

    /** 下载地址的规范形式（数据库/URL 识别均以它为锚点） */
    public static final String DOWNLOAD_PATTERN = "/admin/system/file/%d/download";

    /** 预览地址（match /app/** 白名单，本身无需登录） */
    private String previewPath = "/app/file/preview";
    /** 签名密钥，生产环境必须覆盖 */
    private String secret = "routego-dev-temp-url-secret";
    /** 临时链接有效期 */
    private Duration ttl = Duration.ofHours(2);

    /** 签发一条临时预览链接 */
    public String build(Long fileId) {
        long expires = Instant.now().getEpochSecond() + ttl.toSeconds();
        return previewPath + "?fid=" + fileId + "&expires=" + expires + "&sign=" + sign(fileId, expires);
    }

    /** HMAC-SHA256(fileId:expires) 的十六进制签名 */
    public String sign(long fileId, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal((fileId + ":" + expires).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("临时链接签名失败", e);
        }
    }

    /** 校验签名与有效期（常量时间比较，防时序攻击） */
    public boolean verify(long fileId, long expires, String sign) {
        if (sign == null || sign.isEmpty() || expires <= Instant.now().getEpochSecond()) {
            return false;
        }
        byte[] expected = sign(fileId, expires).getBytes(StandardCharsets.UTF_8);
        byte[] actual = sign.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    /** 把任意形式的文件地址还原为标准下载地址；识别不出时原样返回 */
    public String canonicalize(String url) {
        Long fileId = extractFileId(url);
        return fileId == null ? url : DOWNLOAD_PATTERN.formatted(fileId);
    }

    /**
     * 从文件地址提取文件 ID。支持：
     * <ul>
     *   <li>标准下载地址：{@code /admin/system/file/123/download}</li>
     *   <li>带 /api 网关前缀或绝对地址：{@code http://h/api/admin/system/file/123/download}</li>
     *   <li>本类签发的临时链接：{@code /app/file/preview?fid=123&expires=..&sign=..}</li>
     * </ul>
     * 识别不出返回 {@code null}（如 /covers/rg0.png 这类静态资源）。
     */
    public static Long extractFileId(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        Matcher preview = PREVIEW_FID.matcher(url);
        if (preview.find()) {
            return Long.valueOf(preview.group(1));
        }
        Matcher download = DOWNLOAD_FID.matcher(url);
        if (download.find()) {
            return Long.valueOf(download.group(1));
        }
        return null;
    }

    /** 临时链接中的 fid 参数（带边界，避免匹配到 expires 的数字） */
    private static final Pattern PREVIEW_FID = Pattern.compile("[?&]fid=(\\d+)(?:&|$)");
    /** 下载地址（兼容可选的绝对地址前缀与 /api 网关前缀） */
    private static final Pattern DOWNLOAD_FID =
            Pattern.compile("(?:https?://[^/]+)?(?:/api)?/admin/system/file/(\\d+)/download");
}
