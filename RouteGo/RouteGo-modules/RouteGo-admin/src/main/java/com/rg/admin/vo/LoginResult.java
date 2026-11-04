package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功返回的 Token 信息。
 */
@Schema(description = "登录成功返回的 Token 信息")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResult {

    @Schema(description = "Token 名称，通常为 Authorization", example = "Authorization")
    private String tokenName;

    @Schema(description = "Token 值", example = "Bearer 5f9c...")
    private String tokenValue;

    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    @Schema(description = "登录账号", example = "admin")
    private String username;

    @Schema(description = "昵称", example = "超级管理员")
    private String nickname;
}
