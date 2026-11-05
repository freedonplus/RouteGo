package com.rg.customer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券视图对象。
 * <p>
 * 券的过期时间不在 coupon 表里存，而是由模板的有效期规则 + 领取时间推导，
 * 因此这里把模板的关键字段一并带出，前端不需要二次查询。
 */
@Schema(description = "用户优惠券")
@Data
public class CouponVO {

    @Schema(description = "券 ID")
    private Long id;

    @Schema(description = "券码")
    private String code;

    @Schema(description = "券模板 ID")
    private Long templateId;

    @Schema(description = "券名称")
    private String templateName;

    @Schema(description = "类型：1 满减，2 折扣")
    private Byte type;

    @Schema(description = "类型描述")
    private String typeDesc;

    @Schema(description = "使用门槛")
    private BigDecimal threshold;

    @Schema(description = "减免金额（满减券）")
    private BigDecimal amount;

    @Schema(description = "折扣率（折扣券）")
    private BigDecimal discount;

    @Schema(description = "优惠说明")
    private String benefitDesc;

    @Schema(description = "会员 ID")
    private Long customerId;

    @Schema(description = "会员昵称")
    private String customerNickname;

    @Schema(description = "会员手机号（脱敏）")
    private String customerPhone;

    @Schema(description = "库中状态：0 未使用，1 已使用，2 已过期")
    private Byte status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "是否已过期（未使用但超过有效期）")
    private Boolean expired;

    @Schema(description = "是否可用：未使用且未过期")
    private Boolean usable;

    @Schema(description = "过期时间，永久有效时为 null")
    private LocalDateTime expireTime;

    @Schema(description = "领取时间")
    private LocalDateTime receiveTime;

    @Schema(description = "使用时间")
    private LocalDateTime useTime;

    @Schema(description = "使用的订单 ID")
    private Long orderId;

    @Schema(description = "使用的订单号")
    private String orderNo;
}
