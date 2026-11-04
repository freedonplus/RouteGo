package com.rg.admin.service;

import com.rg.admin.vo.CaptchaResponse;
import com.rg.common.captcha.CaptchaGenerator;
import com.rg.common.captcha.CaptchaResult;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {

    @Mock
    private RedisService redis;
    @Mock
    private CaptchaGenerator generator;

    private CaptchaService service;

    @BeforeEach
    void setUp() {
        service = new CaptchaService(generator, redis);
    }

    @Test
    void generateStoresAnswerForTwoMinutes() {
        byte[] png = {1, 2, 3};
        when(generator.generate()).thenReturn(new CaptchaResult("AB3D", png));

        CaptchaResponse response = service.generate();

        assertThat(response.uuid()).isNotBlank();
        assertThat(response.image()).isEqualTo(Base64.getEncoder().encodeToString(png));
        verify(redis).set("admin:Auth:captcha:" + response.uuid(), "AB3D", Duration.ofMinutes(2));
    }

    @Test
    void verifyConsumesOnceAndIsCaseInsensitive() {
        when(redis.getAndDelete("admin:Auth:captcha:u1")).thenReturn("AB3D");

        service.verify("u1", "ab3d");

        verify(redis).getAndDelete("admin:Auth:captcha:u1");
    }

    @Test
    void verifyFailsWhenExpired() {
        when(redis.getAndDelete("admin:Auth:captcha:u2")).thenReturn(null);

        assertThatThrownBy(() -> service.verify("u2", "AB3D"))
                .isInstanceOf(AdminException.class)
                .hasMessageContaining("验证码");
    }

    @Test
    void verifyFailsWhenCodeMismatches() {
        when(redis.getAndDelete("admin:Auth:captcha:u3")).thenReturn("AB3D");

        assertThatThrownBy(() -> service.verify("u3", "ZZZZ"))
                .isInstanceOf(AdminException.class)
                .hasMessageContaining("验证码");
    }
}
