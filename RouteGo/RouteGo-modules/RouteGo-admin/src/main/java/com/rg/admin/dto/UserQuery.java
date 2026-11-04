package com.rg.admin.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统用户分页查询条件。
 */
@Schema(description = "系统用户分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQuery extends PageQuery {

    @Schema(description = "账号，模糊匹配")
    private String username;

    @Schema(description = "昵称，模糊匹配")
    private String nickname;

    @Schema(description = "手机号，精确匹配")
    private String phone;

    @Schema(description = "状态：0 禁用，1 启用")
    private Byte status;

    @Schema(description = "角色 ID")
    private Long roleId;

    @Schema(description = "创建时间起始")
    private LocalDateTime createTimeStart;

    @Schema(description = "创建时间截止")
    private LocalDateTime createTimeEnd;
}
