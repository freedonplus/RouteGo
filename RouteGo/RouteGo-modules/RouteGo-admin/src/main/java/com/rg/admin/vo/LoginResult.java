package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功返回的 Token 信息。
 * <p>
 * {@code tokenValue} 是 Sa-Token 原始 Token；作为请求头使用时需自行拼接
 * {@code Bearer } 前缀，Cookie 模式则由服务端自动处理。
 */
@Schema(description = "登录成功返回的 Token 信息")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResult {

    @Schema(description = "Token 名称，取自 sa-token.token-name，当前为 Authorization",
            example = "Authorization")
    private String tokenName;

    @Schema(description = "Sa-Token 原始 Token（不含 Bearer 前缀）",
            example = "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed")
    private String tokenValue;

    @Schema(description = "用户 ID（sys_user.id）", example = "1")
    private Long userId;

    @Schema(description = "登录账号", example = "your_admin")
    private String username;

    @Schema(description = "昵称", example = "管理员")
    private String nickname;
}
