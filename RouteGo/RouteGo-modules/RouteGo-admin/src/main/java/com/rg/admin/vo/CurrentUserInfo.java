package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前登录用户信息。
 */
@Schema(description = "当前登录用户信息")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserInfo {

    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    @Schema(description = "登录账号", example = "admin")
    private String username;

    @Schema(description = "昵称", example = "超级管理员")
    private String nickname;

    @Schema(description = "角色编码列表", example = "[\"super_admin\"]")
    private List<String> roles;

    @Schema(description = "权限码列表", example = "[\"sys:user:list\"]")
    private List<String> permissions;
}
