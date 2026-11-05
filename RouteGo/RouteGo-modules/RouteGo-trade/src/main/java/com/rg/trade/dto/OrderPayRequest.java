package com.rg.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 订单支付入参。
 * <p>
 * 金额不从这里传：一律按订单快照里的实付金额收款，避免前端改价。
 */
@Schema(description = "订单支付")
@Data
public class OrderPayRequest {

    @Schema(description = "支付方式：1 模拟支付（默认），2 微信支付")
    private Byte payType;

    @Schema(description = "第三方交易号；模拟支付留空即可")
    @Size(max = 100, message = "第三方交易号最多 100 字")
    private String transactionId;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注最多 255 字")
    private String remark;
}
