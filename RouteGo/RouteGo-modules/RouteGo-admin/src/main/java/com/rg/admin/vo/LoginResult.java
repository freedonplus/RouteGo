package com.rg.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功返回的 Token 信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResult {

    private String tokenName;
    private String tokenValue;
    private Long userId;
    private String username;
    private String nickname;
}
