package com.rg.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单视图对象。
 * <p>
 * 列表接口只填主表与客户/线路的展示字段；详情接口额外返回出行人、
 * 状态流水、支付记录与退款记录。
 */
@Schema(description = "订单")
@Data
public class OrderVO {

    private Long id;

    @Schema(description = "订单号", example = "RG20261105000123")
    private String orderNo;

    private Long customerId;

    @Schema(description = "客户昵称")
    private String customerNickname;

    @Schema(description = "客户手机号（脱敏）")
    private String customerPhone;

    private Long routeId;

    private Long departureId;

    @Schema(description = "线路标题快照")
    private String routeTitle;

    @Schema(description = "出发日期快照")
    private LocalDate departDate;

    private Integer travelerCount;

    @Schema(description = "单价快照")
    private BigDecimal unitPrice;

    @Schema(description = "订单总额 = 单价 × 人数")
    private BigDecimal totalAmount;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    @Schema(description = "实付金额 = 总额 - 优惠")
    private BigDecimal payAmount;

    private String contactName;

    @Schema(description = "联系电话（脱敏）")
    private String contactPhone;

    private Byte status;

    @Schema(description = "订单状态描述", example = "待支付")
    private String statusDesc;

    private Byte payStatus;

    @Schema(description = "支付状态描述", example = "未支付")
    private String payStatusDesc;

    private LocalDateTime payTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private String remark;

    private Byte source;

    @Schema(description = "来源描述", example = "后台代录")
    private String sourceDesc;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Schema(description = "出行人明细；仅详情接口返回")
    private List<OrderTravelerVO> travelers;

    @Schema(description = "状态流水；仅详情接口返回")
    private List<OrderStatusLogVO> statusLogs;

    @Schema(description = "支付记录；仅详情接口返回")
    private List<PaymentRecordVO> paymentRecords;

    @Schema(description = "退款记录；仅详情接口返回")
    private List<RefundRecordVO> refundRecords;
}
