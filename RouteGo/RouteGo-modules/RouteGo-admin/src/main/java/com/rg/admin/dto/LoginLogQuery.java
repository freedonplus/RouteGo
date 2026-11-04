package com.rg.admin.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "登录日志分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLogQuery extends PageQuery {

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "登录 IP")
    private String ip;

    @Schema(description = "状态：0 失败，1 成功")
    private Byte status;

    @Schema(description = "起始时间")
    private LocalDateTime startTime;

    @Schema(description = "截止时间")
    private LocalDateTime endTime;
}
