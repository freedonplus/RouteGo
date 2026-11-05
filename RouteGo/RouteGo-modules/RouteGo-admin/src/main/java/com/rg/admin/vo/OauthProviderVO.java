package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 第三方登录平台状态。
 * <p>
 * 登录页需要知道「有哪些平台」而不是「哪些平台能用」——否则一个都没配置时
 * 整个第三方登录入口都会消失，用户无从得知功能是否存在、差什么配置。
 * 因此这里同时返回启用状态与缺失项说明。
 */
@Schema(description = "第三方登录平台")
public class OauthProviderVO {

    @Schema(description = "平台标识，如 github", example = "github")
    private String code;

    @Schema(description = "展示名称", example = "GitHub")
    private String label;

    @Schema(description = "是否已启用且配置完整，可点击跳转")
    private boolean available;

    @Schema(description = "是否已在配置中开启 enabled")
    private boolean enabled;

    @Schema(description = "不可用原因，可用时为空", example = "未配置 GITHUB_CLIENT_ID / GITHUB_CLIENT_SECRET")
    private String reason;

    public OauthProviderVO() {
    }

    public OauthProviderVO(String code, String label, boolean available, boolean enabled, String reason) {
        this.code = code;
        this.label = label;
        this.available = available;
        this.enabled = enabled;
        this.reason = reason;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
