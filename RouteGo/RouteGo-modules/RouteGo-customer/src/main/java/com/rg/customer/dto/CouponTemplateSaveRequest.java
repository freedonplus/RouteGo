package com.rg.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板新增/编辑入参。
 * <p>
 * 满减券必须填 {@link #amount}，折扣券必须填 {@link #discount}，由服务端做类型与字段的一致性校验。
 * 有效期两种模式互斥：固定区间（start_time / end_time）与领取后 N 天（valid_days）。
 */
@Schema(description = "优惠券模板保存入参")
@Data
public class CouponTemplateSaveRequest {

    @Schema(description = "券名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "满 1000 减 100")
    @NotBlank(message = "券名称不能为空")
    @Size(max = 100, message = "券名称最多 100 字")
    private String name;

    @Schema(description = "类型：1 满减，2 折扣", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "券类型不能为空")
    @Min(value = 1, message = "券类型取值为 1/2")
    @Max(value = 2, message = "券类型取值为 1/2")
    private Byte type;

    @Schema(description = "使用门槛（订单金额需达到该值才能用），默认 0 表示无门槛")
    @DecimalMin(value = "0.00", message = "使用门槛不能为负数")
    private BigDecimal threshold;

    @Schema(description = "减免金额，满减券必填")
    @DecimalMin(value = "0.01", message = "减免金额需大于 0")
    private BigDecimal amount;

    @Schema(description = "折扣率，如 0.90 表示九折，折扣券必填")
    @DecimalMin(value = "0.01", message = "折扣率需大于 0")
    @DecimalMax(value = "0.99", message = "折扣率需小于 1，全额减免请用满减券")
    private BigDecimal discount;

    @Schema(description = "发行总量，0 表示不限量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "发行总量不能为空")
    @Min(value = 0, message = "发行总量不能为负数")
    private Integer totalCount;

    @Schema(description = "有效期类型：1 固定区间，2 领取后 N 天", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "有效期类型不能为空")
    @Min(value = 1, message = "有效期类型取值为 1/2")
    @Max(value = 2, message = "有效期类型取值为 1/2")
    private Byte validType;

    @Schema(description = "有效期开始（validType=1 时必填）")
    private LocalDateTime startTime;

    @Schema(description = "有效期结束（validType=1 时必填）")
    private LocalDateTime endTime;

    @Schema(description = "领取后有效天数（validType=2 时必填）")
    @Min(value = 1, message = "有效天数至少 1 天")
    private Integer validDays;

    @Schema(description = "状态：0 停用，1 启用，默认 1")
    @Min(value = 0, message = "状态取值为 0/1")
    @Max(value = 1, message = "状态取值为 0/1")
    private Byte status;
}
