package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.rg.admin.dto.LoginRequest;
import com.rg.admin.service.AuthService;
import com.rg.admin.vo.CurrentUserInfo;
import com.rg.admin.vo.LoginResult;
import com.rg.common.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台登录认证。
 * <p>
 * 登录接口不要求 Token；登出和当前用户接口通过 {@link SaCheckLogin} 校验。
 */
@RestController
@RequestMapping("/admin/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * ------------------------------
     *             登录
     * ------------------------------
     */
    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequest request,
                                     HttpServletRequest servletRequest) {
        return Result.success(authService.login(request, resolveClientIp(servletRequest)));
    }

    /**
     * ------------------------------
     *             登出
     * ------------------------------
     */
    @SaCheckLogin
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }

    /**
     * ------------------------------
     *          当前登录用户
     * ------------------------------
     */
    @SaCheckLogin
    @GetMapping("/me")
    public Result<CurrentUserInfo> me() {
        return Result.success(authService.currentUser());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
