package com.rg.admin.vo;

import com.rg.common.sensitive.Sensitive;
import com.rg.common.sensitive.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户列表项；密码不返回，手机号/邮箱脱敏。
 */
@Schema(description = "系统用户列表项")
@Data
public class SysUserVO {

    @Schema(description = "用户 ID", example = "1")
    private Long id;

    @Schema(description = "登录账号", example = "ops_user")
    private String username;

    @Schema(description = "昵称", example = "运营小王")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "手机号（脱敏）", example = "138****8000")
    @Sensitive(SensitiveType.PHONE)
    private String phone;

    @Schema(description = "邮箱（脱敏）", example = "o***@example.com")
    @Sensitive(SensitiveType.EMAIL)
    private String email;

    @Schema(description = "状态：0 禁用，1 启用", example = "1")
    private Byte status;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录 IP")
    private String lastLoginIp;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "乐观锁版本号", example = "0")
    private Integer version;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
