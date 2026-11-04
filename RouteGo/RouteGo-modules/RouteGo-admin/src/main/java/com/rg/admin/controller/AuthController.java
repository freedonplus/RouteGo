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
 * {@code /login} 不要求 Token；{@code /logout} 和 {@code /me} 通过
 * {@link SaCheckLogin} 校验，未登录返回 401。
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
     * 账号密码登录。
     * <p>
     * 先按 BCrypt 校验密码，再检查账号是否启用；成功后记录登录时间与来源 IP。
     * 浏览器同时收到名为 {@code Authorization} 的 HttpOnly Cookie，App/第三方
     * 使用响应体中的 {@code tokenValue} 走 Bearer 头。
     */
    @Operation(
            summary = "登录",
            description = "BCrypt 校验账号密码，成功后签发 Sa-Token 并记录登录时间与来源 IP；"
                    + "响应头写入 Authorization HttpOnly Cookie。",
            security = {},
            responses = {
                    @ApiResponse(responseCode = "200", description = "登录成功"),
                    @ApiResponse(responseCode = "400", description = "账号或密码为空"),
                    @ApiResponse(responseCode = "401", description = "账号不存在或密码错误"),
                    @ApiResponse(responseCode = "403", description = "账号已被禁用")
            })
    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequest request,
                                     HttpServletRequest servletRequest) {
        return Result.success(authService.login(request, resolveClientIp(servletRequest)));
    }




    /**
     * 注销当前会话，删除服务端存储的 Token。
     */
    @Operation(
            summary = "登出",
            description = "注销当前会话并删除服务端（Redis）中的 Token；需要已登录。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "登出成功"),
                    @ApiResponse(responseCode = "401", description = "未登录或 Token 已失效")
            })
    @SaCheckLogin
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }




    /**
     * 当前登录用户及其角色、权限码，数据来自 Sa-Token 会话。
     */
    @Operation(
            summary = "当前登录用户",
            description = "返回当前登录用户的基本信息、角色编码和权限码；"
                    + "admin/super_admin 角色返回通配权限 *。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功"),
                    @ApiResponse(responseCode = "401", description = "未登录或 Token 已失效")
            })
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
