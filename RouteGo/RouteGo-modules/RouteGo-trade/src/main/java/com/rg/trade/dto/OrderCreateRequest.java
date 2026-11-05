package com.rg.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 后台代录订单入参。
 * <p>
 * 线路标题、出发日期、单价都会在服务端按下单时刻的数据做快照，
 * 之后线路改价改名都不影响已生成的订单。
 */
@Schema(description = "创建订单入参")
@Data
public class OrderCreateRequest {

    @Schema(description = "客户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择客户")
    private Long customerId;

    @Schema(description = "线路 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择线路")
    private Long routeId;

    @Schema(description = "班期 ID；指定后按班期校验库存并占用库存")
    private Long departureId;

    @Schema(description = "出发日期；传了班期时以班期日期为准")
    private LocalDate departDate;

    @Schema(description = "出行人数", example = "2")
    @NotNull(message = "请填写出行人数")
    @Min(value = 1, message = "出行人数至少 1 人")
    @Max(value = 50, message = "单笔订单最多 50 人，更多请拆分订单")
    private Integer travelerCount;

    @Schema(description = "单价；留空时取班期成人价，无班期取线路最低价")
    @DecimalMin(value = "0.00", message = "单价不能为负")
    @Digits(integer = 8, fraction = 2, message = "单价最多 8 位整数、2 位小数")
    private BigDecimal unitPrice;

    @Schema(description = "优惠金额，默认 0")
    @DecimalMin(value = "0.00", message = "优惠金额不能为负")
    @Digits(integer = 8, fraction = 2, message = "优惠金额最多 8 位整数、2 位小数")
    private BigDecimal discountAmount;

    @Schema(description = "联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写联系人")
    @Size(max = 50, message = "联系人姓名最多 50 字")
    private String contactName;

    @Schema(description = "联系电话", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写联系电话")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;

    @Schema(description = "来源：1 后台代录（默认），2 微信小程序")
    private Byte source;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注最多 255 字")
    private String remark;

    @Schema(description = "出行人清单；可下单时一并录入")
    @Valid
    private List<OrderTravelerSaveRequest> travelers;
}
