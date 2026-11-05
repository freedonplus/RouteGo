package com.rg.trade.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 退款记录查询条件。
 */
@Schema(description = "退款记录查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class RefundQuery extends PageQuery {

    @Schema(description = "订单 ID")
    private Long orderId;

    @Schema(description = "退款单号，模糊匹配")
    private String refundNo;

    @Schema(description = "状态：0 待审核，1 已通过，2 已驳回，3 已退款")
    private Byte status;

    @Schema(description = "申请时间起（含），格式 yyyy-MM-dd HH:mm:ss")
    private String applyTimeBegin;

    @Schema(description = "申请时间止（含），格式 yyyy-MM-dd HH:mm:ss")
    private String applyTimeEnd;
}
