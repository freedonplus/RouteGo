package com.rg.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量发券。
 * <p>
 * 整批在一个事务里完成：任何一位会员不可发放（不存在 / 已禁用 / 券已领完）都会整体回滚，
 * 避免出现「发了一半」这种对账困难的中间状态。
 */
@Schema(description = "批量发券入参")
@Data
public class CouponGrantRequest {

    @Schema(description = "优惠券模板 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "券模板不能为空")
    private Long templateId;

    @Schema(description = "接收会员 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "至少选择一位会员")
    @Size(max = 200, message = "单次最多发放 200 人")
    private List<Long> customerIds;
}
