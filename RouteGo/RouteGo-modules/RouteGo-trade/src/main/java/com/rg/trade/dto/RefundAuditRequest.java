package com.rg.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 退款审核入参。
 * <p>
 * 通过后订单转为「已退款」并回退班期库存与客户消费额；驳回则恢复为「已支付」。
 */
@Schema(description = "退款审核")
@Data
public class RefundAuditRequest {

    @Schema(description = "是否通过：true 同意退款，false 驳回", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择审核结果")
    private Boolean approved;

    @Schema(description = "审核意见")
    @Size(max = 255, message = "审核意见最多 255 字")
    private String auditRemark;
}
