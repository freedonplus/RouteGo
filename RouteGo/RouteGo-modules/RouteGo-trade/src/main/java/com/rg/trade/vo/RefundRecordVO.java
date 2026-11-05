package com.rg.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录视图对象。
 */
@Schema(description = "退款记录")
@Data
public class RefundRecordVO {

    private Long id;

    private Long orderId;

    @Schema(description = "关联订单号")
    private String orderNo;

    private String refundNo;

    private BigDecimal amount;

    private String reason;

    private Byte status;

    @Schema(description = "状态描述", example = "待审核")
    private String statusDesc;

    private LocalDateTime applyTime;

    private LocalDateTime auditTime;

    private Long auditBy;

    private String auditRemark;

    private LocalDateTime createTime;
}
