package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.rg.admin.config.AdminSecurityProperties;
import com.rg.admin.dto.ChangePasswordRequest;
import com.rg.admin.dto.CodeLoginRequest;
import com.rg.admin.dto.LoginRequest;
import com.rg.admin.dto.RegisterRequest;
import com.rg.admin.dto.SendCodeRequest;
import com.rg.admin.service.AuthService;
import com.rg.admin.service.CaptchaService;
import com.rg.admin.service.VerificationCodeService;
import com.rg.admin.vo.CaptchaResponse;
import com.rg.admin.vo.CurrentUserInfo;
import com.rg.admin.vo.LoginResult;
import com.rg.admin.vo.SendCodeResult;
import com.rg.common.sms.SmsProperties;
import com.rg.common.util.IpUtils;
import com.rg.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台登录认证。
 * <p>
 * {@code /captcha}、{@code /login} 不要求 Token；其余接口通过
 * {@link SaCheckLogin} 校验，未登录返回 401。
 */
@Tag(name = "后台认证", description = "验证码、登录、登出、当前用户与修改密码")
@RestController
@RequestMapping("/admin/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final VerificationCodeService verificationCodeService;
    private final SmsProperties smsProperties;
    private final AdminSecurityProperties securityProperties;

    public AuthController(AuthService authService,
                          CaptchaService captchaService,
                          VerificationCodeService verificationCodeService,
                          SmsProperties smsProperties,
                          AdminSecurityProperties securityProperties) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.verificationCodeService = verificationCodeService;
        this.smsProperties = smsProperties;
        this.securityProperties = securityProperties;
    }

    /**
     * 生成 4 位字符验证码，答案在 Redis 保存 2 分钟且只能使用一次。
     */
    @Operation(summary = "获取图形验证码", description = "返回验证码标识与 PNG 图片的 Base64 编码。",
            security = {})
    @GetMapping("/captcha")
    public Result<CaptchaResponse> captcha() {
        return Result.success(captchaService.generate());
    }

    /**
     * 账号密码登录。
     * <p>
     * 校验顺序：验证码 → 失败锁定 → 账号密码 → 账号状态；成功后记录登录时间与来源 IP。
     * 浏览器同时收到名为 {@code Authorization} 的 HttpOnly Cookie。
     */
    @Operation(
            summary = "登录",
            description = "校验验证码与账号密码（BCrypt），成功后签发 Sa-Token 并记录登录时间与来源 IP；"
                    + "响应头写入 Authorization HttpOnly Cookie。",
            security = {},
            responses = {
                    @ApiResponse(responseCode = "200", description = "登录成功"),
                    @ApiResponse(responseCode = "400", description = "验证码错误或参数为空"),
                    @ApiResponse(responseCode = "401", description = "账号不存在或密码错误"),
                    @ApiResponse(responseCode = "403", description = "账号已被禁用"),
                    @ApiResponse(responseCode = "429", description = "登录失败次数过多，已锁定")
            })
    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequest request,
                                     HttpServletRequest servletRequest) {
        return Result.success(authService.login(request, resolveClientIp(servletRequest)));
    }

    /**
     * 发送登录 / 绑定 / 重置密码验证码。
     * <p>
     * 接收方支持手机号（走短信网关）与邮箱（走 SMTP）。同接收方 60 秒冷却、
     * 每日限 20 条，同 IP 每小时限 20 条。
     */
    @Operation(summary = "发送验证码", description = "向手机号或邮箱发送 6 位验证码，返回脱敏后的接收方。",
            security = {},
            responses = {
                    @ApiResponse(responseCode = "200", description = "发送成功"),
                    @ApiResponse(responseCode = "400", description = "接收方格式不正确"),
                    @ApiResponse(responseCode = "429", description = "发送过于频繁"),
                    @ApiResponse(responseCode = "503", description = "短信 / 邮件服务未启用或发送失败")
            })
    @PostMapping("/code")
    public Result<SendCodeResult> sendCode(@Valid @RequestBody SendCodeRequest request,
                                           HttpServletRequest servletRequest) {
        String masked = verificationCodeService.send(
                request.getTarget(), parseScene(request.getScene()), resolveClientIp(servletRequest));
        return Result.success(new SendCodeResult(masked, smsProperties.getResendInterval().toSeconds()));
    }

    /**
     * 验证码登录（短信 / 邮箱），账号必须已绑定对应手机号或邮箱。
     */
    @Operation(summary = "验证码登录", description = "用手机号或邮箱收到的验证码登录，成功后签发 Sa-Token。",
            security = {},
            responses = {
                    @ApiResponse(responseCode = "200", description = "登录成功"),
                    @ApiResponse(responseCode = "400", description = "验证码错误或已失效"),
                    @ApiResponse(responseCode = "401", description = "该手机号 / 邮箱未绑定账号"),
                    @ApiResponse(responseCode = "403", description = "账号已被禁用")
            })
    @PostMapping("/login/code")
    public Result<LoginResult> loginByCode(@Valid @RequestBody CodeLoginRequest request,
                                           HttpServletRequest servletRequest) {
        return Result.success(authService.loginByCode(
                request.getTarget(), request.getCode(), resolveClientIp(servletRequest)));
    }

    /**
     * 手机号验证码注册：创建账号并默认绑定「普通用户」角色，成功后直接登录。
     */
    @Operation(summary = "注册", description = "手机号 + 短信验证码注册新账号，默认绑定普通用户角色并直接签发登录态。",
            security = {},
            responses = {
                    @ApiResponse(responseCode = "200", description = "注册成功并已登录"),
                    @ApiResponse(responseCode = "400", description = "验证码错误或已失效"),
                    @ApiResponse(responseCode = "409", description = "手机号已注册"),
                    @ApiResponse(responseCode = "503", description = "默认角色未初始化")
            })
    @PostMapping("/register")
    public Result<LoginResult> register(@Valid @RequestBody RegisterRequest request,
                                        HttpServletRequest servletRequest) {
        return Result.success(authService.register(request, resolveClientIp(servletRequest)));
    }

    private VerificationCodeService.Scene parseScene(String scene) {
        if (scene == null || scene.isBlank()) {
            return VerificationCodeService.Scene.LOGIN;
        }
        try {
            return VerificationCodeService.Scene.valueOf(scene.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return VerificationCodeService.Scene.LOGIN;
        }
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

    /**
     * 修改本人密码，成功后所有会话失效。
     */
    @Operation(
            summary = "修改本人密码",
            description = "校验旧密码与新密码强度（8–20 位，至少含字母和数字），"
                    + "修改成功后该用户全部会话失效。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "修改成功"),
                    @ApiResponse(responseCode = "400", description = "旧密码错误或新密码强度不足"),
                    @ApiResponse(responseCode = "401", description = "未登录")
            })
    @SaCheckLogin
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }

    private String resolveClientIp(HttpServletRequest request) {
        return IpUtils.resolveClientIp(request, securityProperties.getTrustedProxies());
    }
}
