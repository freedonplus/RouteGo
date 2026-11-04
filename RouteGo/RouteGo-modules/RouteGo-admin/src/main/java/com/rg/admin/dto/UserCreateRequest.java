package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 新增系统用户入参。
 */
@Schema(description = "新增系统用户入参")
@Data
public class UserCreateRequest {

    @Schema(description = "登录账号，3–50 位字母/数字/下划线，创建后不可修改",
            example = "ops_user", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入账号")
    @Pattern(regexp = "^[A-Za-z0-9_]{3,50}$", message = "账号需为 3–50 位字母、数字或下划线")
    private String username;

    @Schema(description = "初始密码：8–20 位，至少包含字母和数字",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入初始密码")
    private String password;

    @Schema(description = "昵称", example = "运营小王")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "手机号（明文入库，输出脱敏）", example = "13812348000")
    private String phone;

    @Schema(description = "邮箱", example = "ops@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "角色 ID 列表，至少一个", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "至少分配一个角色")
    private List<Long> roleIds;
}
