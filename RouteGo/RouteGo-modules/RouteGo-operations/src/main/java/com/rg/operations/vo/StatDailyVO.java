package com.rg.operations.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日经营统计视图对象。
 */
@Schema(description = "每日经营统计")
@Data
public class StatDailyVO {

    @Schema(description = "记录 ID")
    private Long id;

    @Schema(description = "统计日期")
    private LocalDate statDate;

    @Schema(description = "当日新建订单数（含已取消）")
    private Integer orderCount;

    @Schema(description = "当日支付成功订单数")
    private Integer paidOrderCount;

    @Schema(description = "当日支付金额（元）")
    private BigDecimal salesAmount;

    @Schema(description = "当日退款金额（元）")
    private BigDecimal refundAmount;

    @Schema(description = "当日净收入（支付金额 − 退款金额）")
    private BigDecimal netAmount;

    @Schema(description = "当日新增会员数")
    private Integer newCustomerCount;

    @Schema(description = "客单价（支付金额 ÷ 支付订单数）")
    private BigDecimal avgOrderAmount;

    @Schema(description = "数据生成时间")
    private LocalDateTime updateTime;
}
