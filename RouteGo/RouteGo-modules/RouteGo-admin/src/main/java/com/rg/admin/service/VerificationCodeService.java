package com.rg.admin.service;

/**
 * 验证码收发与校验。
 * <p>
 * 按「场景 + 接收方」隔离存储，同一验证码不能跨场景复用（登录码不能拿去改密码）。
 * 发送前做三重限流：同接收方冷却、同接收方日上限、同 IP 小时上限。
 */
public interface VerificationCodeService {

    /**
     * 验证码使用场景，参与缓存 key 的命名空间。
     */
    enum Scene {
        /** 验证码登录。 */
        LOGIN,
        /** 注册新账号。 */
        REGISTER,
        /** 绑定手机号 / 邮箱。 */
        BIND,
        /** 找回密码。 */
        RESET_PASSWORD
    }

    /**
     * 发送验证码。
     *
     * @param target   手机号或邮箱
     * @param scene    场景
     * @param clientIp 客户端 IP，用于限流
     * @return 脱敏后的接收方，供前端展示
     */
    String send(String target, Scene scene, String clientIp);

    /**
     * 校验并消费验证码，失败抛 {@code AdminException}。
     */
    void verify(String target, Scene scene, String code);
}
