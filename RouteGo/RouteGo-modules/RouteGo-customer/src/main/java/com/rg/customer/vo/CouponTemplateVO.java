package com.rg.customer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板视图对象。
 */
@Schema(description = "优惠券模板")
@Data
public class CouponTemplateVO {

    @Schema(description = "模板 ID")
    private Long id;

    @Schema(description = "券名称")
    private String name;

    @Schema(description = "类型：1 满减，2 折扣")
    private Byte type;

    @Schema(description = "类型描述")
    private String typeDesc;

    @Schema(description = "使用门槛")
    private BigDecimal threshold;

    @Schema(description = "减免金额（满减券）")
    private BigDecimal amount;

    @Schema(description = "折扣率（折扣券），如 0.90")
    private BigDecimal discount;

    @Schema(description = "优惠说明，如「满 1000.00 减 100.00」「满 500.00 享 9 折」")
    private String benefitDesc;

    @Schema(description = "发行总量，0 表示不限量")
    private Integer totalCount;

    @Schema(description = "已领取数量")
    private Integer receivedCount;

    @Schema(description = "剩余可发放数量，不限量时为 null")
    private Integer remainCount;

    @Schema(description = "有效期类型：1 固定区间，2 领取后 N 天")
    private Byte validType;

    @Schema(description = "有效期类型描述")
    private String validTypeDesc;

    @Schema(description = "有效期开始")
    private LocalDateTime startTime;

    @Schema(description = "有效期结束")
    private LocalDateTime endTime;

    @Schema(description = "领取后有效天数")
    private Integer validDays;

    @Schema(description = "有效期说明")
    private String validDesc;

    @Schema(description = "状态：0 停用，1 启用")
    private Byte status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "是否已过有效期")
    private Boolean expired;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
