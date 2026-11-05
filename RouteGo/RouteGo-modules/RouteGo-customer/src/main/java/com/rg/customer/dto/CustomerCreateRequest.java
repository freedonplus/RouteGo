package com.rg.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 后台代注册会员。
 * <p>
 * 手机号是会员的唯一身份标识，落库前会用 AES 加密并生成盲索引。
 * 密码可留空——小程序用户走短信验证码登录，不需要密码。
 */
@Schema(description = "会员新增入参")
@Data
public class CustomerCreateRequest {

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "登录密码，留空表示不设密码（仅支持验证码登录）")
    @Size(min = 8, max = 32, message = "密码长度需在 8~32 位之间")
    private String password;

    @Schema(description = "昵称")
    @Size(max = 50, message = "昵称最多 50 字")
    private String nickname;

    @Schema(description = "头像地址")
    @Size(max = 255, message = "头像地址最多 255 字")
    private String avatar;

    @Schema(description = "真实姓名")
    @Size(max = 50, message = "真实姓名最多 50 字")
    private String realName;

    @Schema(description = "证件号，落库加密")
    @Size(max = 32, message = "证件号最多 32 字")
    private String idCard;

    @Schema(description = "性别：0 未知，1 男，2 女")
    @Min(value = 0, message = "性别取值为 0/1/2")
    @Max(value = 2, message = "性别取值为 0/1/2")
    private Byte gender;

    @Schema(description = "生日")
    private LocalDate birthday;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最多 100 字")
    private String email;

    @Schema(description = "会员等级：1 普通，2 银卡，3 金卡，4 钻石，默认 1")
    @Min(value = 1, message = "会员等级取值为 1~4")
    @Max(value = 4, message = "会员等级取值为 1~4")
    private Byte level;

    @Schema(description = "初始积分，默认 0")
    @Min(value = 0, message = "积分不能为负数")
    private Integer points;

    @Schema(description = "状态：0 禁用，1 正常，默认 1")
    @Min(value = 0, message = "状态取值为 0/1")
    @Max(value = 1, message = "状态取值为 0/1")
    private Byte status;
}
