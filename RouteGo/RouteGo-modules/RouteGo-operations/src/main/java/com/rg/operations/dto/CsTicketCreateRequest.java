package com.rg.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新建工单入参。
 * <p>
 * 工单号由服务端生成，不接受外部传入；客户与订单是可选的关联对象，
 * 传了会校验存在性，避免挂到不存在的订单上导致后续跟进查无对应记录。
 */
@Schema(description = "新建客服工单")
@Data
public class CsTicketCreateRequest {

    @Schema(description = "客户 ID，可空（例如电话来访未注册）")
    private Long customerId;

    @Schema(description = "关联订单 ID，可空")
    private Long orderId;

    @Schema(description = "类型：1 售前，2 订单，3 退款，4 投诉，5 其他", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工单类型不能为空")
    @Min(value = 1, message = "工单类型取值为 1~5")
    @Max(value = 5, message = "工单类型取值为 1~5")
    private Byte type;

    @Schema(description = "工单标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "行程临时改期怎么操作")
    @NotBlank(message = "工单标题不能为空")
    @Size(max = 100, message = "工单标题最多 100 个字符")
    private String title;

    @Schema(description = "问题描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "问题描述不能为空")
    private String content;

    @Schema(description = "优先级：1 低，2 中，3 高，默认中")
    @Min(value = 1, message = "优先级取值为 1~3")
    @Max(value = 3, message = "优先级取值为 1~3")
    private Byte priority;

    @Schema(description = "处理人 ID，可空；指派后工单自动进入「处理中」")
    private Long handlerId;
}
