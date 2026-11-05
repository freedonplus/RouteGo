package com.rg.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 修改会员资料。
 * <p>
 * 手机号是身份标识，不在此修改，需走单独的换绑接口；
 * 密码、积分也不在这里改，避免运营误操作篡改会员资产。
 */
@Schema(description = "会员编辑入参")
@Data
public class CustomerUpdateRequest {

    @Schema(description = "昵称")
    @Size(max = 50, message = "昵称最多 50 字")
    private String nickname;

    @Schema(description = "头像地址")
    @Size(max = 255, message = "头像地址最多 255 字")
    private String avatar;

    @Schema(description = "真实姓名")
    @Size(max = 50, message = "真实姓名最多 50 字")
    private String realName;

    @Schema(description = "证件号，落库加密；传空字符串表示清空")
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
}
