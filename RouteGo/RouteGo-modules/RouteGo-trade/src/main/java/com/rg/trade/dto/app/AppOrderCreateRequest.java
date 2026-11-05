package com.rg.trade.dto.app;

import com.rg.trade.dto.OrderTravelerSaveRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * C 端下单入参。
 * <p>
 * 与后台代录的差别：{@code customerId} 不接受客户端传值，一律取登录态；
 * {@code source} 固定为 2（App），便于按渠道统计。
 */
@Schema(description = "App 创建订单")
@Data
public class AppOrderCreateRequest {

    @Schema(description = "线路 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择线路")
    private Long routeId;

    @Schema(description = "班期 ID；指定后按班期校验库存并占用库存")
    private Long departureId;

    @Schema(description = "出行人数", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请填写出行人数")
    @Min(value = 1, message = "出行人数至少 1 人")
    @Max(value = 50, message = "单笔订单最多 50 人，更多请拆分订单")
    private Integer travelerCount;

    @Schema(description = "联系人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写联系人")
    @Size(max = 50, message = "联系人最多 50 字")
    private String contactName;

    @Schema(description = "联系电话", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写联系电话")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注最多 255 字")
    private String remark;

    @Schema(description = "出行人清单；可下单时一并录入")
    @Valid
    private List<OrderTravelerSaveRequest> travelers;
}
