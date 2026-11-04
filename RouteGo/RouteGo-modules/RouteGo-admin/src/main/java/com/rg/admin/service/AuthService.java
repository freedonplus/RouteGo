package com.rg.admin.service;

import com.rg.admin.dto.LoginRequest;
import com.rg.admin.vo.CurrentUserInfo;
import com.rg.admin.vo.LoginResult;

/**
 * 后台登录认证服务。
 */
public interface AuthService {

    LoginResult login(LoginRequest request, String clientIp);

    CurrentUserInfo currentUser();

    /**
     * 修改当前登录用户密码，成功后所有会话失效。
     */
    void changePassword(String oldPassword, String newPassword);
}
