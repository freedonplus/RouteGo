package com.rg.customer.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * C 端验证码发送结果。
 */
@Schema(description = "App 验证码发送结果")
@Data
@AllArgsConstructor
public class AppCodeVO {

    @Schema(description = "脱敏后的接收手机号", example = "138****8000")
    private String maskedPhone;

    @Schema(description = "多少秒后可重发", example = "60")
    private long resendAfter;
}
