package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "第三方账号绑定")
@Data
public class SysUserOauthVO {
    private Long id;
    private Long userId;
    private String provider;
    private String openid;
    private String unionid;
    private String nickname;
    private String avatar;
    private LocalDateTime bindTime;
}
