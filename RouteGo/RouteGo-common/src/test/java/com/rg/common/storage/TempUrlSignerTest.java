package com.rg.common.storage;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TempUrlSignerTest {

    private final TempUrlSigner signer = new TempUrlSigner();

    @Test
    void buildProducesSignedPreviewUrlAndVerifyPasses() {
        String url = signer.build(42L);

        assertThat(url).startsWith("/app/file/preview?fid=42&").contains("expires=").contains("sign=");

        long fid = 42L;
        long expires = Long.parseLong(url.replaceAll(".*expires=(\\d+).*", "$1"));
        String sign = url.replaceAll(".*sign=([0-9a-f]+)$", "$1");
        assertThat(signer.verify(fid, expires, sign)).isTrue();
    }

    @Test
    void verifyRejectsTamperedSignAndExpiredLink() {
        long expires = Instant.now().getEpochSecond() + 60;
        String sign = signer.sign(1L, expires);

        assertThat(signer.verify(1L, expires, sign + "ff")).isFalse();
        assertThat(signer.verify(2L, expires, sign)).isFalse();
        assertThat(signer.verify(1L, Instant.now().getEpochSecond() - 10, sign)).isFalse();
        assertThat(signer.verify(1L, expires, null)).isFalse();
    }

    @Test
    void customTtlAndSecretTakeEffect() {
        TempUrlSigner custom = new TempUrlSigner();
        custom.setTtl(Duration.ofMinutes(5));
        custom.setSecret("another-secret");

        String url = custom.build(9L);
        long expires = Long.parseLong(url.replaceAll(".*expires=(\\d+).*", "$1"));
        String sign = url.replaceAll(".*sign=([0-9a-f]+)$", "$1");

        // 同参数但不同密钥的签名不能互认
        assertThat(signer.verify(9L, expires, sign)).isFalse();
        assertThat(custom.verify(9L, expires, sign)).isTrue();
        // TTL 体现在 expires 上（约 5 分钟，留 1 分钟余量避免抖动）
        long delta = expires - Instant.now().getEpochSecond();
        assertThat(delta).isBetween(Duration.ofMinutes(4).toSeconds(), Duration.ofMinutes(5).toSeconds());
    }

    @Test
    void extractFileIdUnderstandsAllUrlShapes() {
        assertThat(TempUrlSigner.extractFileId("/admin/system/file/123/download")).isEqualTo(123L);
        assertThat(TempUrlSigner.extractFileId("/api/admin/system/file/123/download")).isEqualTo(123L);
        assertThat(TempUrlSigner.extractFileId("http://192.168.1.5:8080/api/admin/system/file/123/download"))
                .isEqualTo(123L);
        String temp = signer.build(7L);
        assertThat(TempUrlSigner.extractFileId(temp)).isEqualTo(7L);
        assertThat(TempUrlSigner.extractFileId("/covers/rg0.png")).isNull();
        assertThat(TempUrlSigner.extractFileId("http://cdn.example.com/a.png")).isNull();
        assertThat(TempUrlSigner.extractFileId(null)).isNull();
        assertThat(TempUrlSigner.extractFileId("")).isNull();
    }

    @Test
    void canonicalizeRestoresDownloadUrl() {
        assertThat(signer.canonicalize(signer.build(7L))).isEqualTo("/admin/system/file/7/download");
        assertThat(signer.canonicalize("/api/admin/system/file/7/download")).isEqualTo("/admin/system/file/7/download");
        // 非文件地址原样保留
        assertThat(signer.canonicalize("/covers/rg0.png")).isEqualTo("/covers/rg0.png");
        assertThat(signer.canonicalize(null)).isNull();
    }
}
