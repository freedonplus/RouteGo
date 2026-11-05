package com.rg.admin.service;

import com.rg.admin.dto.LoginRequest;
import com.rg.admin.dto.RegisterRequest;
import com.rg.admin.vo.CurrentUserInfo;
import com.rg.admin.vo.LoginResult;

/**
 * 后台登录认证服务。
 */
public interface AuthService {

    /**
     * 账号密码登录。{@code account} 支持用户名、手机号、邮箱三种形式。
     */
    LoginResult login(LoginRequest request, String clientIp);

    /**
     * 验证码登录（短信 / 邮箱）。账号必须已绑定对应手机号或邮箱。
     */
    LoginResult loginByCode(String target, String code, String clientIp);

    /**
     * 手机号验证码注册：创建账号并默认绑定「普通用户」角色，成功后直接签发登录态。
     */
    LoginResult register(RegisterRequest request, String clientIp);

    /**
     * 已完成外部身份校验（如第三方登录）时直接签发会话，跳过密码校验。
     * <p>
     * 只允许在服务端已经确认身份的场景调用，不要在 Controller 里直接暴露。
     */
    LoginResult loginTrusted(Long userId, String clientIp, String remark);

    CurrentUserInfo currentUser();

    /**
     * 修改当前登录用户密码，成功后所有会话失效。
     */
    void changePassword(String oldPassword, String newPassword);
}
