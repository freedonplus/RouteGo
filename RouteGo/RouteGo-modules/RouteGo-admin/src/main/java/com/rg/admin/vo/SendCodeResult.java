package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 验证码发送结果。
 */
@Schema(description = "验证码发送结果")
public record SendCodeResult(
        @Schema(description = "脱敏后的接收方") String target,
        @Schema(description = "多少秒后可重新发送") long resendAfterSeconds) {
}
