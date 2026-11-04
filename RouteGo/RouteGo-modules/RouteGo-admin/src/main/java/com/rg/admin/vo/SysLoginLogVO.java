package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "登录日志")
@Data
public class SysLoginLogVO {
    private Long id;
    private String username;
    private String ip;
    private String browser;
    private String os;
    private Byte status;
    private String msg;
    private LocalDateTime loginTime;
}
