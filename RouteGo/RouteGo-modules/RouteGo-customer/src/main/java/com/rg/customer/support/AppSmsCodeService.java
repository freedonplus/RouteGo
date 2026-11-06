package com.rg.customer.support;

import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.BusinessException;
import com.rg.common.redis.RedisService;
import com.rg.common.sms.SmsClient;
import com.rg.common.sms.SmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * C 端会员短信验证码。
 * <p>
 * 与后台的 {@code VerificationCodeService} 分开存放：Key 前缀不同，互不干扰，
 * 也避免 App 验证码能用于后台登录。限流同样按手机号维度做重发间隔。
 */
@Slf4j
@Service
public class AppSmsCodeService {

    /** 验证码缓存前缀 */
    private static final String CODE_KEY = "app:sms:code:";
    /** 重发限流前缀 */
    private static final String LIMIT_KEY = "app:sms:limit:";
    /** 单个验证码最多校验失败的次数，超过即作废，防暴力猜解 */
    private static final String FAIL_KEY = "app:sms:fail:";
    private static final int MAX_FAIL = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RedisService redisService;
    /**
     * 短信客户端按需取：{@code routego.sms.enabled=false} 时容器里根本没有这个 Bean。
     * 直接注入 SmsClient 会让「关闭短信」的生产环境启动失败，所以这里用 ObjectProvider。
     */
    private final ObjectProvider<SmsClient> smsClientProvider;
    private final SmsProperties smsProperties;

    public AppSmsCodeService(RedisService redisService, ObjectProvider<SmsClient> smsClientProvider,
                             SmsProperties smsProperties) {
        this.redisService = redisService;
        this.smsClientProvider = smsClientProvider;
        this.smsProperties = smsProperties;
    }

    /**
     * 发送登录验证码，返回脱敏手机号。
     *
     * @throws BusinessException 短信未启用、发送过于频繁或网关失败
     */
    public String send(String phone) {
        SmsClient smsClient = smsClientProvider.getIfAvailable();
        if (!smsProperties.isEnabled() || smsClient == null) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "短信服务未启用，请联系管理员");
        }
        if (Boolean.TRUE.equals(redisService.hasKey(LIMIT_KEY + phone))) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS,
                    "验证码已发送，请 " + smsProperties.getResendInterval().toSeconds() + " 秒后再试");
        }
        String code = randomCode(smsProperties.getCodeLength());
        // 先发短信再落缓存：发送失败不占用重发额度
        smsClient.sendVerificationCode(phone, code);
        redisService.set(CODE_KEY + phone, code, smsProperties.getCodeTtl());
        redisService.set(LIMIT_KEY + phone, "1", smsProperties.getResendInterval());
        redisService.delete(FAIL_KEY + phone);
        return maskPhone(phone);
    }

    /**
     * 校验验证码，成功后立即失效，防止重复使用。
     */
    public void verify(String phone, String code) {
        String cached = redisService.get(CODE_KEY + phone);
        if (cached == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码已失效，请重新获取");
        }
        if (!cached.equals(code)) {
            Long fails = redisService.incrementWithTtl(FAIL_KEY + phone, 1, Duration.ofMinutes(10));
            if (fails != null && fails >= MAX_FAIL) {
                redisService.delete(CODE_KEY + phone);
                redisService.delete(FAIL_KEY + phone);
                throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码错误次数过多，请重新获取");
            }
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码不正确");
        }
        redisService.delete(CODE_KEY + phone);
        redisService.delete(FAIL_KEY + phone);
    }

    private String randomCode(int length) {
        int len = length > 0 ? length : 6;
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
