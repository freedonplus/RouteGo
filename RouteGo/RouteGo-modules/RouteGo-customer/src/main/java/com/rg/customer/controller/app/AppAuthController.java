package com.rg.customer.controller.app;

import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.BusinessException;
import com.rg.common.security.StpKit;
import com.rg.common.sms.SmsProperties;
import com.rg.common.util.Result;
import com.rg.customer.dto.app.AppAccountLoginRequest;
import com.rg.customer.dto.app.AppCodeRequest;
import com.rg.customer.dto.app.AppLoginRequest;
import com.rg.customer.dto.app.AppPasswordLoginRequest;
import com.rg.customer.dto.app.AppRegisterRequest;
import com.rg.customer.dto.app.AppSetPasswordRequest;
import com.rg.customer.service.CustomerService;
import com.rg.customer.support.AppSmsCodeService;
import com.rg.customer.vo.CustomerVO;
import com.rg.customer.vo.app.AppCodeVO;
import com.rg.customer.vo.app.AppLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端会员认证。
 * <p>
 * 与后台 {@code /admin/auth/**} 完全隔离：这里是 customer 账号体系，
 * Token 走 {@code customer-token} 请求头；手机号未注册时自动建号。
 * 业务逻辑都在 {@link CustomerService} 与 {@link AppSmsCodeService}，本类只做编排。
 */
@Slf4j
@Tag(name = "App-会员认证", description = "手机号验证码登录 / 当前会员")
@RestController
@RequestMapping("/app/auth")
public class AppAuthController {

    private final CustomerService customerService;
    private final AppSmsCodeService smsCodeService;
    private final SmsProperties smsProperties;

    public AppAuthController(CustomerService customerService,
                             AppSmsCodeService smsCodeService,
                             SmsProperties smsProperties) {
        this.customerService = customerService;
        this.smsCodeService = smsCodeService;
        this.smsProperties = smsProperties;
    }

    @Operation(summary = "发送登录验证码", description = "同号 60 秒内不可重发；短信服务未启用时返回 503。")
    @PostMapping("/code")
    public Result<AppCodeVO> sendCode(@Valid @RequestBody AppCodeRequest request) {
        String masked = smsCodeService.send(request.getPhone().trim());
        return Result.success(new AppCodeVO(masked, smsProperties.getResendInterval().toSeconds()));
    }

    @Operation(summary = "验证码登录", description = "手机号未注册时自动创建会员；返回的 token 放入 customer-token 请求头。")
    @PostMapping("/login")
    public Result<AppLoginVO> login(@Valid @RequestBody AppLoginRequest request) {
        String phone = request.getPhone().trim();
        smsCodeService.verify(phone, request.getCode());

        CustomerVO customer = customerService.findByPhone(phone);
        boolean newCustomer = false;
        if (customer == null) {
            Long id = customerService.registerByPhone(phone);
            customer = customerService.detail(id);
            newCustomer = true;
            log.info("App 新会员注册 customerId={}", id);
        }
        if (customer.getStatus() != null && customer.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用，请联系客服");
        }

        StpKit.CUSTOMER.login(customer.getId());
        return Result.success(buildLogin(customer, newCustomer));
    }

    /** 组装登录返回：token 放入 Authorization 头即可（与全局 sa-token.token-name 一致） */
    private AppLoginVO buildLogin(CustomerVO customer, boolean newCustomer) {
        AppLoginVO vo = new AppLoginVO();
        vo.setToken(StpKit.CUSTOMER.getTokenValue());
        vo.setTokenName("Authorization");
        vo.setExpiresIn(StpKit.CUSTOMER.getTokenTimeout());
        vo.setNewCustomer(newCustomer);
        // 未设密码的账号只能用验证码登录，前端拿到 false 后引导补设
        vo.setHasPassword(customerService.hasPassword(customer.getId()));
        vo.setCustomer(customer);
        return vo;
    }

    @Operation(summary = "账号密码登录",
            description = "账号一个输入框：手机号或用户名都行。账号未设密码时提示先用验证码登录并补设。")
    @PostMapping("/login/account")
    public Result<AppLoginVO> loginByAccount(@Valid @RequestBody AppAccountLoginRequest request) {
        CustomerVO customer = customerService.loginByAccount(request.getAccount().trim(), request.getPassword());
        StpKit.CUSTOMER.login(customer.getId());
        return Result.success(buildLogin(customer, false));
    }

    @Operation(summary = "设置/修改登录密码", description = "需登录态。验证码登录后若账号无密码，前端调用此接口补设。")
    @PostMapping("/password")
    public Result<Void> setPassword(@Valid @RequestBody AppSetPasswordRequest request) {
        StpKit.CUSTOMER.checkLogin();
        customerService.ensurePassword(StpKit.currentCustomerId(), request.getPassword());
        return Result.success();
    }

    @Operation(summary = "密码登录", description = "手机号 + BCrypt 密码；未设过密码的账号会提示改用验证码登录。")
    @PostMapping("/login/password")
    public Result<AppLoginVO> loginByPassword(@Valid @RequestBody AppPasswordLoginRequest request) {
        CustomerVO customer = customerService.loginByPassword(request.getPhone().trim(), request.getPassword());
        StpKit.CUSTOMER.login(customer.getId());
        return Result.success(buildLogin(customer, false));
    }

    @Operation(summary = "注册并设置密码", description = "手机号验证码确认身份：未注册自动建号，已注册但没密码则补设；返回登录态。")
    @PostMapping("/register")
    public Result<AppLoginVO> register(@Valid @RequestBody AppRegisterRequest request) {
        String phone = request.getPhone().trim();
        smsCodeService.verify(phone, request.getCode());

        CustomerVO customer = customerService.findByPhone(phone);
        Long customerId;
        if (customer == null) {
            // 先校验用户名是否被占用，避免建号后再回滚（会产生一个没密码的孤儿账号）
            if (request.getNickname() != null && !request.getNickname().isBlank()) {
                CustomerVO taken = customerService.findByAccount(request.getNickname().trim());
                if (taken != null) {
                    throw new BusinessException(ErrorCode.CONFLICT, "该用户名已被使用，换一个试试");
                }
            }
            customerId = customerService.registerByPhone(phone);
            log.info("App 注册新会员 customerId={}", customerId);
        } else if (customer.getStatus() != null && customer.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用，请联系客服");
        } else {
            customerId = customer.getId();
        }
        customerService.ensurePassword(customerId, request.getPassword());
        // 注册时填的用户名同时作为密码登录用的账号
        customerService.setNickname(customerId, request.getNickname());
        StpKit.CUSTOMER.login(customerId);
        return Result.success(buildLogin(customerService.detail(customerId), customer == null));
    }

    @Operation(summary = "当前会员", description = "需要 customer-token 登录态。")
    @GetMapping("/me")
    public Result<CustomerVO> me() {
        StpKit.CUSTOMER.checkLogin();
        return Result.success(customerService.detail(StpKit.currentCustomerId()));
    }

    @Operation(summary = "退出登录", description = "注销当前 customer 会话。")
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpKit.CUSTOMER.logout();
        return Result.success();
    }
}
