package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 图形验证码响应。
 *
 * @param uuid  验证码标识，登录时原样回传
 * @param image PNG 图片的 Base64 编码（不含 data URI 前缀）
 */
@Schema(description = "图形验证码响应")
public record CaptchaResponse(
        @Schema(description = "验证码标识，登录时原样回传", example = "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed")
        String uuid,
        @Schema(description = "PNG 图片的 Base64 编码（不含 data URI 前缀）")
        String image) {
}
