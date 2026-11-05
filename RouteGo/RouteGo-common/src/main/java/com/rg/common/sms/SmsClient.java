package com.rg.common.sms;

/**
 * 短信发送客户端。
 */
public interface SmsClient {

    /**
     * 发送验证码短信。
     *
     * @param phone 接收手机号
     * @param code  验证码内容
     * @return 服务商请求编号，可能为 {@code null}
     */
    String sendVerificationCode(String phone, String code);
}
