package com.rg.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rg.admin.config.OauthProperties;
import com.rg.admin.entity.SysUser;
import com.rg.admin.entity.SysUserOauth;
import com.rg.admin.mapper.SysUserMapper;
import com.rg.admin.mapper.SysUserOauthMapper;
import com.rg.admin.service.AuthService;
import com.rg.admin.service.OauthService;
import com.rg.admin.vo.LoginResult;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.redis.RedisService;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthDingTalkRequest;
import me.zhyd.oauth.request.AuthGiteeRequest;
import me.zhyd.oauth.request.AuthGithubRequest;
import me.zhyd.oauth.request.AuthQqRequest;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthWeChatOpenRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 第三方登录实现。
 * <p>
 * 流程：{@code authorizeUrl} 生成带 state 的授权地址 → 前端跳转 → 平台回调后端
 * → 校验 state → 用 code 换 openid → 查 {@code sys_user_oauth} 绑定关系 → 登录。
 * 未绑定时不自动建号：后台账号涉及权限，必须由管理员显式绑定。
 */
@Service
public class OauthServiceImpl implements OauthService {

    private static final Logger log = LoggerFactory.getLogger(OauthServiceImpl.class);
    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final OauthProperties properties;
    private final SysUserOauthMapper oauthMapper;
    private final SysUserMapper userMapper;
    private final AuthService authService;
    private final RedisService redisService;

    public OauthServiceImpl(OauthProperties properties,
                            SysUserOauthMapper oauthMapper,
                            SysUserMapper userMapper,
                            AuthService authService,
                            RedisService redisService) {
        this.properties = properties;
        this.oauthMapper = oauthMapper;
        this.userMapper = userMapper;
        this.authService = authService;
        this.redisService = redisService;
    }

    @Override
    public String authorizeUrl(String provider) {
        AuthRequest request = buildRequest(provider);
        String state = AuthStateUtils.createState();
        redisService.set(String.format(CacheKeyConstants.Admin.OAUTH_STATE, state),
                provider, STATE_TTL);
        return request.authorize(state);
    }

    @Override
    public LoginResult login(String provider, String code, String state, String clientIp) {
        AuthUser authUser = exchange(provider, code, state);
        SysUserOauth binding = oauthMapper.selectOne(Wrappers.<SysUserOauth>lambdaQuery()
                .eq(SysUserOauth::getProvider, provider)
                .eq(SysUserOauth::getOpenid, authUser.getUuid())
                .last("LIMIT 1"));
        if (binding == null) {
            log.info("第三方账号未绑定 provider={} openid={}", provider, authUser.getUuid());
            throw new AdminException(ErrorCode.UNAUTHORIZED,
                    "该" + providerLabel(provider) + "账号尚未绑定后台用户，请先用账号密码登录后绑定");
        }
        SysUser user = userMapper.selectById(binding.getUserId());
        if (user == null) {
            throw new AdminException(ErrorCode.UNAUTHORIZED, "绑定关系指向的用户已被删除");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new AdminException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        return authService.loginTrusted(user.getId(), clientIp, providerLabel(provider) + "登录");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindCurrentUser(String provider, String code, String state) {
        long userId = StpUtil.getLoginIdAsLong();
        AuthUser authUser = exchange(provider, code, state);

        SysUserOauth existing = oauthMapper.selectOne(Wrappers.<SysUserOauth>lambdaQuery()
                .eq(SysUserOauth::getProvider, provider)
                .eq(SysUserOauth::getOpenid, authUser.getUuid())
                .last("LIMIT 1"));
        if (existing != null) {
            if (existing.getUserId().equals(userId)) {
                return;
            }
            throw new AdminException(ErrorCode.CONFLICT, "该第三方账号已被其他后台用户绑定");
        }

        SysUserOauth binding = new SysUserOauth();
        binding.setUserId(userId);
        binding.setProvider(provider);
        binding.setOpenid(authUser.getUuid());
        binding.setUnionid(extractUnionId(authUser));
        binding.setNickname(authUser.getNickname());
        binding.setAvatar(authUser.getAvatar());
        binding.setBindTime(LocalDateTime.now());
        oauthMapper.insert(binding);
    }

    /**
     * JustAuth 的 AuthUser 没有统一的 unionId 访问器，微信/QQ 的开放平台 unionid
     * 只出现在原始响应里，这里做一次兼容提取。
     */
    private String extractUnionId(AuthUser user) {
        try {
            com.alibaba.fastjson.JSONObject raw = user.getRawUserInfo();
            if (raw == null) {
                return null;
            }
            if (raw.containsKey("unionid")) {
                return raw.getString("unionid");
            }
            com.alibaba.fastjson.JSONObject data = raw.getJSONObject("data");
            return data == null ? null : data.getString("unionid");
        } catch (RuntimeException e) {
            return null;
        }
    }

    private AuthUser exchange(String provider, String code, String state) {        if (!StringUtils.hasText(code)) {
            throw new AdminException(ErrorCode.BAD_REQUEST, "缺少授权码");
        }
        verifyState(provider, state);
        AuthResponse<AuthUser> response;
        try {
            response = buildRequest(provider).login(AuthCallback.builder()
                    .code(code)
                    .state(state)
                    .build());
        } catch (RuntimeException e) {
            log.error("第三方登录调用失败 provider={}", provider, e);
            throw new AdminException(ErrorCode.SERVICE_UNAVAILABLE, "第三方登录服务暂不可用");
        }
        if (!response.ok() || response.getData() == null) {
            log.warn("第三方登录失败 provider={} msg={}", provider, response.getMsg());
            throw new AdminException(ErrorCode.UNAUTHORIZED,
                    "第三方登录失败：" + (response.getMsg() == null ? "未知错误" : response.getMsg()));
        }
        return response.getData();
    }

    private void verifyState(String provider, String state) {
        if (!StringUtils.hasText(state)) {
            throw new AdminException(ErrorCode.BAD_REQUEST, "缺少 state 参数");
        }
        String key = String.format(CacheKeyConstants.Admin.OAUTH_STATE, state);
        String cached = redisService.get(key);
        if (cached == null) {
            throw new AdminException(ErrorCode.BAD_REQUEST, "授权已过期，请重新发起登录");
        }
        if (!cached.equalsIgnoreCase(provider)) {
            throw new AdminException(ErrorCode.BAD_REQUEST, "state 与平台不匹配");
        }
        redisService.getAndDelete(key);
    }

    private AuthRequest buildRequest(String provider) {
        OauthProperties.Provider config = properties.getProviders().get(provider);
        if (config == null || !config.isEnabled()
                || !StringUtils.hasText(config.getClientId())
                || !StringUtils.hasText(config.getClientSecret())) {
            throw new AdminException(ErrorCode.SERVICE_UNAVAILABLE,
                    providerLabel(provider) + "登录未配置，请先在配置文件中填写 client-id / client-secret");
        }
        AuthConfig authConfig = AuthConfig.builder()
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret())
                .redirectUri(config.getRedirectUri())
                .build();
        return switch (provider) {
            case "github" -> new AuthGithubRequest(authConfig);
            case "gitee" -> new AuthGiteeRequest(authConfig);
            case "wechat" -> new AuthWeChatOpenRequest(authConfig);
            case "qq" -> new AuthQqRequest(authConfig);
            case "dingtalk" -> new AuthDingTalkRequest(authConfig);
            default -> throw new AdminException(ErrorCode.BAD_REQUEST, "不支持的第三方平台：" + provider);
        };
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
