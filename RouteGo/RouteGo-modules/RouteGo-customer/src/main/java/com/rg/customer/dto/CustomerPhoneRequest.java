package com.rg.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 会员手机号换绑。
 * <p>
 * 换绑会同时改写密文列与盲索引列，因此单独开接口，
 * 与资料编辑分离开，便于在操作日志里追踪敏感变更。
 */
@Schema(description = "会员手机号换绑入参")
@Data
public class CustomerPhoneRequest {

    @Schema(description = "新手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13900139000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
