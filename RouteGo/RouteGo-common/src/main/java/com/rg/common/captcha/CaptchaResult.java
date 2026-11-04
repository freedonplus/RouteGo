package com.rg.common.captcha;

/**
 * 一次验证码生成结果：答案明文 + PNG 图片字节。
 */
public record CaptchaResult(String code, byte[] png) {
}
