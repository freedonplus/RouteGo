package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.rg.admin.dto.LoginRequest;
import com.rg.admin.service.AuthService;
import com.rg.admin.vo.CurrentUserInfo;
import com.rg.admin.vo.LoginResult;
import com.rg.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "后台认证", description = "后台登录、登出与当前用户信息")
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
    @Operation(
            summary = "登录",
            description = "校验账号密码，成功后返回 Token；浏览器同时写入 HttpOnly Cookie。",
            security = {},
            responses = {
                    @ApiResponse(responseCode = "200", description = "登录成功"),
                    @ApiResponse(responseCode = "401", description = "账号或密码错误"),
                    @ApiResponse(responseCode = "403", description = "账号已被禁用")
            })
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
    @Operation(summary = "登出", description = "注销当前会话并清理 Token。")
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
    @Operation(summary = "当前登录用户", description = "返回当前登录用户的基本信息、角色与权限码。")
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
