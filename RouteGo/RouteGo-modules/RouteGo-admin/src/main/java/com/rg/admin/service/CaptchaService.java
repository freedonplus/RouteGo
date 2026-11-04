package com.rg.admin.service;

import com.rg.admin.vo.CaptchaResponse;
import com.rg.common.captcha.CaptchaGenerator;
import com.rg.common.captcha.CaptchaResult;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.redis.RedisService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * 图形验证码的生成与一次性校验。
 */
@Service
public class CaptchaService {

    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(2);

    private final CaptchaGenerator generator;
    private final RedisService redisService;

    public CaptchaService(CaptchaGenerator generator, RedisService redisService) {
        this.generator = generator;
        this.redisService = redisService;
    }

    public CaptchaResponse generate() {
        CaptchaResult result = generator.generate();
        String uuid = UUID.randomUUID().toString();
        redisService.set(String.format(CacheKeyConstants.Admin.CAPTCHA, uuid),
                result.code(), CAPTCHA_TTL);
        return new CaptchaResponse(uuid, Base64.getEncoder().encodeToString(result.png()));
    }

    /**
     * 校验并立即消费验证码；无论成功失败，同一个 uuid 都不能再次使用。
     */
    public void verify(String uuid, String code) {
        if (!StringUtils.hasText(uuid)) {
            throw new AdminException("验证码已失效，请刷新后重试");
        }
        String expected = redisService.getAndDelete(String.format(CacheKeyConstants.Admin.CAPTCHA, uuid));
        if (expected == null || !StringUtils.hasText(code) || !expected.equalsIgnoreCase(code.trim())) {
            throw new AdminException("验证码错误或已过期");
        }
    }
}
