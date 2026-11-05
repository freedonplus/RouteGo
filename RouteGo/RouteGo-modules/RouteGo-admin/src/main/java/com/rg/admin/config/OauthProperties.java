package com.rg.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第三方登录配置。
 * <p>
 * 每个平台一组 {@code client-id / client-secret / redirect-uri}，未配置的平台视为未启用，
 * 前端调用时会返回明确提示。密钥同样支持环境变量覆盖。
 */
@ConfigurationProperties(prefix = "routego.oauth")
public class OauthProperties {

    /** 登录成功后跳回的前端地址（会拼接 ?token=...）。 */
    private String successRedirect = "http://localhost:5173/login?oauth=success";

    /** 登录失败后跳回的前端地址（会拼接 &message=...）。 */
    private String failureRedirect = "http://localhost:5173/login?oauth=failed";

    /** 平台配置，key 为平台标识：github / gitee / wechat / qq / dingtalk。 */
    private Map<String, Provider> providers = new LinkedHashMap<>();

    public String getSuccessRedirect() {
        return successRedirect;
    }

    public void setSuccessRedirect(String successRedirect) {
        this.successRedirect = successRedirect;
    }

    public String getFailureRedirect() {
        return failureRedirect;
    }

    public void setFailureRedirect(String failureRedirect) {
        this.failureRedirect = failureRedirect;
    }

    public Map<String, Provider> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, Provider> providers) {
        this.providers = providers;
    }

    /**
     * 单个平台配置。
     */
    public static class Provider {

        private boolean enabled = false;
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        /** 钉钉专用。 */
        private String agentId;
        private String scopes;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getScopes() {
            return scopes;
        }

        public void setScopes(String scopes) {
            this.scopes = scopes;
        }
    }
}
