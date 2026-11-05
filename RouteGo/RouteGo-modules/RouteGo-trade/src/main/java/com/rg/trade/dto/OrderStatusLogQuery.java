package com.rg.trade.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单状态流水查询条件。
 */
@Schema(description = "订单状态流水查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderStatusLogQuery extends PageQuery {

    @Schema(description = "订单 ID")
    private Long orderId;

    @Schema(description = "目标状态：0 待支付，1 已支付，2 出行中，3 已完成，4 已取消，5 退款中，6 已退款")
    private Byte toStatus;

    @Schema(description = "操作人名称，模糊匹配")
    private String operatorName;

    @Schema(description = "时间起（含），格式 yyyy-MM-dd HH:mm:ss")
    private String createTimeBegin;

    @Schema(description = "时间止（含），格式 yyyy-MM-dd HH:mm:ss")
    private String createTimeEnd;
}
