package com.rg.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款申请入参。
 */
@Schema(description = "退款申请")
@Data
public class RefundApplyRequest {

    @Schema(description = "退款金额，不能超过实付金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请填写退款金额")
    @DecimalMin(value = "0.01", message = "退款金额必须大于 0")
    @Digits(integer = 8, fraction = 2, message = "退款金额最多 8 位整数、2 位小数")
    private BigDecimal amount;

    @Schema(description = "退款原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写退款原因")
    @Size(max = 255, message = "退款原因最多 255 字")
    private String reason;
}
