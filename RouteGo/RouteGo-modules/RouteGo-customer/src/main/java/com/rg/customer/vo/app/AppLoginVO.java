package com.rg.customer.vo.app;

import com.rg.customer.vo.CustomerVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * C 端登录结果。token 需放入请求头 {@code Authorization: Bearer <token>}。
 */
@Schema(description = "App 登录结果")
@Data
public class AppLoginVO {

    @Schema(description = "登录令牌（不含 Bearer 前缀）")
    private String token;

    @Schema(description = "令牌请求头名称", example = "Authorization")
    private String tokenName;

    @Schema(description = "有效期（秒）")
    private long expiresIn;

    @Schema(description = "是否本次新注册")
    private boolean newCustomer;

    @Schema(description = "是否已设置登录密码；false 时前端应引导补设，否则密码登录不可用")
    private boolean hasPassword;

    @Schema(description = "会员信息（手机号已脱敏）")
    private CustomerVO customer;
}
