package com.rg.trade.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 支付记录查询条件。
 */
@Schema(description = "支付记录查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentQuery extends PageQuery {

    @Schema(description = "订单 ID")
    private Long orderId;

    @Schema(description = "支付单号，模糊匹配")
    private String payNo;

    @Schema(description = "支付方式：1 模拟，2 微信支付")
    private Byte payType;

    @Schema(description = "状态：0 失败，1 成功")
    private Byte status;

    @Schema(description = "支付时间起（含），格式 yyyy-MM-dd HH:mm:ss")
    private String payTimeBegin;

    @Schema(description = "支付时间止（含），格式 yyyy-MM-dd HH:mm:ss")
    private String payTimeEnd;
}
