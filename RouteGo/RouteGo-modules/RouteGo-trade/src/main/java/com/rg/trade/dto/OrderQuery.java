package com.rg.trade.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 订单分页查询条件。
 */
@Schema(description = "订单分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderQuery extends PageQuery {

    @Schema(description = "订单号，模糊匹配")
    private String orderNo;

    @Schema(description = "客户 ID")
    private Long customerId;

    @Schema(description = "线路 ID")
    private Long routeId;

    @Schema(description = "班期 ID")
    private Long departureId;

    @Schema(description = "订单状态：0 待支付，1 已支付，2 出行中，3 已完成，4 已取消，5 退款中，6 已退款")
    private Byte status;

    @Schema(description = "支付状态：0 未支付，1 已支付，2 已退款")
    private Byte payStatus;

    @Schema(description = "来源：1 后台代录，2 微信小程序")
    private Byte source;

    @Schema(description = "联系人姓名，模糊匹配")
    private String contactName;

    @Schema(description = "联系电话，精确匹配（按盲索引查询，不做模糊）")
    private String contactPhone;

    @Schema(description = "出发日期起（含）")
    private LocalDate departDateBegin;

    @Schema(description = "出发日期止（含）")
    private LocalDate departDateEnd;

    @Schema(description = "下单时间起（含），格式 yyyy-MM-dd HH:mm:ss")
    private String createTimeBegin;

    @Schema(description = "下单时间止（含），格式 yyyy-MM-dd HH:mm:ss")
    private String createTimeEnd;
}
