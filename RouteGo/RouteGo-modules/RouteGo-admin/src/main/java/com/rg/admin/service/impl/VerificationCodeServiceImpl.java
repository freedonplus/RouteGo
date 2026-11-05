package com.rg.admin.service.impl;

import com.rg.admin.service.VerificationCodeService;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.redis.RedisService;
import com.rg.common.sms.SmsClient;
import com.rg.common.sms.SmsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * 验证码实现：Redis 存储 + 多重限流 + 短信/邮件双通道。
 * <p>
 * 限流维度：接收方冷却（60s）→ 接收方日上限 → 单 IP 小时上限。冷却与场景无关，
 * 防止「换个场景名」绕过冷却刷短信。
 */
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeServiceImpl.class);

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ATTEMPTS = 5;
    /** 冷却 key 占位场景：冷却按接收方全局生效。 */
    private static final String COOLDOWN_SCOPE = "any";

    private final RedisService redisService;
    private final SmsProperties smsProperties;
    private final ObjectProvider<SmsClient> smsClientProvider;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailFrom;

    public VerificationCodeServiceImpl(RedisService redisService,
                                       SmsProperties smsProperties,
                                       ObjectProvider<SmsClient> smsClientProvider,
                                       ObjectProvider<JavaMailSender> mailSenderProvider,
                                       @Value("${spring.mail.username:no-reply@routego.local}") String mailFrom) {
        this.redisService = redisService;
        this.smsProperties = smsProperties;
        this.smsClientProvider = smsClientProvider;
        this.mailSenderProvider = mailSenderProvider;
        this.mailFrom = mailFrom;
    }

    @Override
    public String send(String target, Scene scene, String clientIp) {
        String receiver = normalize(target);
        Channel channel = channelOf(receiver);
        String cooldownKey = acquireQuota(receiver, clientIp);
        String code = randomCode();
        try {
            deliver(channel, receiver, code, scene);
        } catch (RuntimeException e) {
            // 发送失败立刻解除冷却，避免用户被一次失败请求锁住 60 秒
            redisService.delete(cooldownKey);
            throw e;
        }
        redisService.set(String.format(CacheKeyConstants.Admin.VERIFY_CODE, scene.name(), receiver),
                code, smsProperties.getCodeTtl());
        redisService.delete(String.format(CacheKeyConstants.Admin.VERIFY_CODE_ATTEMPT,
                scene.name(), receiver));
        return mask(receiver);
    }

    @Override
    public void verify(String target, Scene scene, String code) {
        String receiver = normalize(target);
        if (!StringUtils.hasText(code)) {
            throw new AdminException(ErrorCode.BAD_REQUEST, "请输入验证码");
        }
        String codeKey = String.format(CacheKeyConstants.Admin.VERIFY_CODE, scene.name(), receiver);
        String cached = redisService.get(codeKey);
        if (cached == null) {
            throw new AdminException(ErrorCode.BAD_REQUEST, "验证码已失效，请重新获取");
        }
        if (!cached.equals(code.trim())) {
            String attemptKey = String.format(CacheKeyConstants.Admin.VERIFY_CODE_ATTEMPT,
                    scene.name(), receiver);
            Long attempts = redisService.incrementWithTtl(attemptKey, 1, smsProperties.getCodeTtl());
            if (attempts != null && attempts >= MAX_ATTEMPTS) {
                // 连续错 5 次直接作废，防暴力枚举
                redisService.delete(codeKey);
                redisService.delete(attemptKey);
                throw new AdminException(ErrorCode.BAD_REQUEST, "验证码错误次数过多，请重新获取");
            }
            throw new AdminException(ErrorCode.BAD_REQUEST, "验证码错误");
        }
        // 一次性使用
        redisService.getAndDelete(codeKey);
    }

    /**
     * 申请发送配额，成功返回冷却 key（发送失败时用于回滚）。
     */
    private String acquireQuota(String receiver, String clientIp) {
        String cooldownKey = String.format(CacheKeyConstants.Admin.VERIFY_CODE_COOLDOWN,
                COOLDOWN_SCOPE, receiver);
        Boolean acquired = redisService.setIfAbsent(cooldownKey, "1", smsProperties.getResendInterval());
        if (!Boolean.TRUE.equals(acquired)) {
            throw new AdminException(ErrorCode.TOO_MANY_REQUESTS, "验证码已发送，请稍后再试");
        }

        Long daily = redisService.incrementWithTtl(String.format(
                        CacheKeyConstants.Admin.VERIFY_CODE_DAILY, receiver),
                1, Duration.ofDays(1));
        if (daily != null && daily > smsProperties.getDailyLimit()) {
            redisService.delete(cooldownKey);
            throw new AdminException(ErrorCode.TOO_MANY_REQUESTS, "该账号今日验证码发送次数已达上限");
        }

        if (StringUtils.hasText(clientIp)) {
            Long perIp = redisService.incrementWithTtl(String.format(
                            CacheKeyConstants.Admin.VERIFY_CODE_IP, clientIp),
                    1, Duration.ofHours(1));
            if (perIp != null && perIp > smsProperties.getHourlyLimitPerIp()) {
                redisService.delete(cooldownKey);
                throw new AdminException(ErrorCode.TOO_MANY_REQUESTS, "当前网络验证码请求过于频繁");
            }
        }
        return cooldownKey;
    }

    private void deliver(Channel channel, String receiver, String code, Scene scene) {
        if (channel == Channel.SMS) {
            SmsClient client = smsClientProvider.getIfAvailable();
            if (client == null) {
                throw new AdminException(ErrorCode.SERVICE_UNAVAILABLE, "短信服务未启用，请联系管理员");
            }
            client.sendVerificationCode(receiver, code);
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            throw new AdminException(ErrorCode.SERVICE_UNAVAILABLE, "邮件服务未配置，暂不支持邮箱验证码");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(receiver);
        message.setSubject("RouteGo " + sceneSubject(scene) + "验证码");
        message.setText("您的验证码是：" + code + "，"
                + smsProperties.getCodeTtl().toMinutes() + " 分钟内有效。如非本人操作请忽略本邮件。");
        try {
            sender.send(message);
        } catch (RuntimeException e) {
            log.error("验证码邮件发送失败 to={}", receiver, e);
            throw new AdminException(ErrorCode.SERVICE_UNAVAILABLE, "邮件发送失败，请稍后重试");
        }
    }

    private String sceneSubject(Scene scene) {
        return switch (scene) {
            case LOGIN -> "登录";
            case REGISTER -> "注册";
            case BIND -> "绑定";
            case RESET_PASSWORD -> "重置密码";
        };
    }

    private String normalize(String target) {
        if (!StringUtils.hasText(target)) {
            throw new AdminException(ErrorCode.BAD_REQUEST, "请输入手机号或邮箱");
        }
        return target.trim();
    }

    private Channel channelOf(String target) {
        if (PHONE.matcher(target).matches()) {
            return Channel.SMS;
        }
        if (EMAIL.matcher(target).matches()) {
            return Channel.EMAIL;
        }
        throw new AdminException(ErrorCode.BAD_REQUEST, "请输入正确的手机号或邮箱");
    }

    private String randomCode() {
        int length = Math.max(4, Math.min(8, smsProperties.getCodeLength()));
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }

    private String mask(String receiver) {
        if (receiver.contains("@")) {
            int at = receiver.indexOf('@');
            String name = receiver.substring(0, at);
            String domain = receiver.substring(at);
            String maskedName = name.length() <= 2
                    ? name.charAt(0) + "*"
                    : name.charAt(0) + "***" + name.charAt(name.length() - 1);
            return maskedName + domain;
        }
        return receiver.substring(0, 3) + "****" + receiver.substring(receiver.length() - 4);
    }

    private enum Channel {
        SMS, EMAIL
    }
}
