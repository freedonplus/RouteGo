package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前登录用户信息，角色与权限码来自 Sa-Token 会话。
 */
@Schema(description = "当前登录用户信息")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserInfo {

    @Schema(description = "用户 ID（sys_user.id）", example = "1")
    private Long userId;

    @Schema(description = "登录账号", example = "your_admin")
    private String username;

    @Schema(description = "昵称", example = "管理员")
    private String nickname;

    @Schema(description = "角色编码列表；admin/super_admin 角色拥有通配权限 *",
            example = "[\"super_admin\"]")
    private List<String> roles;

    @Schema(description = "权限码列表；admin/super_admin 角色返回 *",
            example = "[\"*\"]")
    private List<String> permissions;
}
