package com.rg.admin.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.rg.admin.config.OauthProperties;
import com.rg.admin.config.AdminSecurityProperties;
import com.rg.admin.service.OauthService;
import com.rg.admin.vo.OauthProviderVO;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.util.IpUtils;
import com.rg.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 第三方登录。
 * <p>
 * 前端流程：取授权地址 → 跳转平台 → 平台回调 {@code callback} → 后端 302 回前端登录页，
 * 浏览器同时拿到 HttpOnly Cookie，前端随后调用 {@code /admin/auth/me} 获取身份。
 */
@Tag(name = "第三方登录", description = "扫码登录与账号绑定")
@RestController
@RequestMapping("/admin/auth/oauth")
public class OauthController {

    private static final Logger log = LoggerFactory.getLogger(OauthController.class);

    private final OauthService oauthService;
    private final OauthProperties oauthProperties;
    private final AdminSecurityProperties securityProperties;

    public OauthController(OauthService oauthService,
                           OauthProperties oauthProperties,
                           AdminSecurityProperties securityProperties) {
        this.oauthService = oauthService;
        this.oauthProperties = oauthProperties;
        this.securityProperties = securityProperties;
    }

    /** 平台展示顺序及缺失配置时提示的环境变量。 */
    private static final Map<String, String[]> PROVIDER_META = new LinkedHashMap<>();

    static {
        PROVIDER_META.put("github", new String[]{"GITHUB_CLIENT_ID", "GITHUB_CLIENT_SECRET"});
        PROVIDER_META.put("gitee", new String[]{"GITEE_CLIENT_ID", "GITEE_CLIENT_SECRET"});
        PROVIDER_META.put("wechat", new String[]{"WECHAT_OPEN_APP_ID", "WECHAT_OPEN_APP_SECRET"});
        PROVIDER_META.put("qq", new String[]{"QQ_APP_ID", "QQ_APP_KEY"});
        PROVIDER_META.put("dingtalk", new String[]{"DINGTALK_APP_KEY", "DINGTALK_APP_SECRET"});
    }

    /**
     * 第三方平台列表，供登录页渲染按钮。
     * <p>
     * 返回全部已登记平台（含未配置的），未配置的以 {@code available=false} 置灰展示，
     * 避免一个都没配时整个入口消失、用户看不到功能存在。
     */
    @Operation(summary = "第三方平台列表", description = "返回全部平台的启用状态与缺失项说明。", security = {})
    @GetMapping("/providers")
    public Result<List<OauthProviderVO>> providers() {
        List<OauthProviderVO> result = new ArrayList<>();
        PROVIDER_META.forEach((code, env) -> {
            OauthProperties.Provider config = oauthProperties.getProviders().get(code);
            boolean enabled = config != null && config.isEnabled();
            boolean configured = config != null
                    && StringUtils.hasText(config.getClientId())
                    && StringUtils.hasText(config.getClientSecret());
            String reason = null;
            if (!enabled) {
                reason = "未启用：请将 routego.oauth.providers." + code + ".enabled 设为 true";
            } else if (!configured) {
                reason = "未配置 " + String.join(" / ", env);
            }
            result.add(new OauthProviderVO(code, providerLabel(code), enabled && configured, enabled, reason));
        });
        return Result.success(result);
    }

    /**
     * 生成某个平台的授权地址（含 state）。
     */
    @Operation(summary = "获取授权地址", security = {})
    @GetMapping("/{provider}/url")
    public Result<String> authorizeUrl(@Parameter(description = "平台标识") @PathVariable String provider) {
        return Result.success(oauthService.authorizeUrl(provider));
    }

    /**
     * 平台回调：成功后 302 回前端登录页，失败同样跳回并带上原因。
     */
    @Operation(summary = "第三方登录回调", security = {})
    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(@PathVariable String provider,
                                         @RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         HttpServletRequest servletRequest) {
        try {
            oauthService.login(provider, code, state,
                    IpUtils.resolveClientIp(servletRequest, securityProperties.getTrustedProxies()));
            return redirect(oauthProperties.getSuccessRedirect());
        } catch (AdminException e) {
            log.warn("第三方登录失败 provider={} reason={}", provider, e.getMessage());
            return redirect(appendMessage(oauthProperties.getFailureRedirect(), e.getMessage()));
        }
    }

    /**
     * 把当前登录用户的账号与第三方账号绑定（需已登录）。
     */
    @Operation(summary = "绑定第三方账号", description = "需已登录；用平台回调拿到的 code 完成绑定。")
    @PostMapping("/{provider}/bind")
    public Result<Void> bind(@PathVariable String provider,
                             @RequestParam String code,
                             @RequestParam String state) {
        StpUtil.checkLogin();
        oauthService.bindCurrentUser(provider, code, state);
        return Result.success();
    }

    private ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location)
                .build();
    }

    private String appendMessage(String base, String message) {
        URI uri = UriComponentsBuilder.fromUriString(base)
                .queryParam("message", message == null ? "登录失败" : message)
                .build()
                .encode()
                .toUri();
        return uri.toString();
    }

    private String providerLabel(String provider) {
        return switch (provider) {
            case "github" -> "GitHub";
            case "gitee" -> "Gitee";
            case "wechat" -> "微信";
            case "qq" -> "QQ";
            case "dingtalk" -> "钉钉";
            default -> provider;
        };
    }
}
