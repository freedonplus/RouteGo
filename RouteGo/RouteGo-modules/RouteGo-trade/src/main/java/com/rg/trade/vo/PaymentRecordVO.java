package com.rg.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录视图对象。
 */
@Schema(description = "支付记录")
@Data
public class PaymentRecordVO {

    private Long id;

    private Long orderId;

    @Schema(description = "关联订单号")
    private String orderNo;

    private String payNo;

    private Byte payType;

    @Schema(description = "支付方式描述", example = "模拟支付")
    private String payTypeDesc;

    private BigDecimal amount;

    private Byte status;

    @Schema(description = "状态描述", example = "成功")
    private String statusDesc;

    private LocalDateTime payTime;

    private String transactionId;

    private LocalDateTime createTime;
}
