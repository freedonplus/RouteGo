package com.rg.admin.service;

import com.rg.admin.vo.LoginResult;

/**
 * 第三方登录（JustAuth 统一封装）。
 */
public interface OauthService {

    /**
     * 生成平台授权地址，同时把 state 写入 Redis 防 CSRF。
     */
    String authorizeUrl(String provider);

    /**
     * 回调换取用户信息并登录；未绑定后台用户时抛出 401。
     */
    LoginResult login(String provider, String code, String state, String clientIp);

    /**
     * 把当前登录用户的账号与第三方账号绑定。
     */
    void bindCurrentUser(String provider, String code, String state);
}
