package com.rg.operations.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 统计区间汇总。
 * <p>
 * 与逐日明细不同，汇总是把区间内的原始流水直接聚合，不依赖 stat_daily 是否已重算，
 * 因此即使某天漏跑定时任务，汇总数字也是准的。
 */
@Schema(description = "经营统计区间汇总")
@Data
public class StatSummaryVO {

    @Schema(description = "区间起始日期")
    private LocalDate startDate;

    @Schema(description = "区间结束日期")
    private LocalDate endDate;

    @Schema(description = "订单数")
    private Long orderCount;

    @Schema(description = "支付成功订单数")
    private Long paidOrderCount;

    @Schema(description = "支付金额（元）")
    private BigDecimal salesAmount;

    @Schema(description = "退款金额（元）")
    private BigDecimal refundAmount;

    @Schema(description = "净收入（元）")
    private BigDecimal netAmount;

    @Schema(description = "新增会员数")
    private Long newCustomerCount;

    @Schema(description = "客单价（元）")
    private BigDecimal avgOrderAmount;
}
