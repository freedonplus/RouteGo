package com.rg.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑工单入参。
 * <p>
 * 全部字段可空：null 表示「不改这一项」，空字符串表示清空（标题除外，标题不允许清空）。
 * 状态与处理人不在这里改，避免和状态机、指派接口的审计语义混在一起。
 */
@Schema(description = "编辑客服工单")
@Data
public class CsTicketUpdateRequest {

    @Schema(description = "客户 ID，null 表示不修改")
    private Long customerId;

    @Schema(description = "关联订单 ID，null 表示不修改")
    private Long orderId;

    @Schema(description = "类型：1 售前，2 订单，3 退款，4 投诉，5 其他")
    @Min(value = 1, message = "工单类型取值为 1~5")
    @Max(value = 5, message = "工单类型取值为 1~5")
    private Byte type;

    @Schema(description = "工单标题")
    @Size(max = 100, message = "工单标题最多 100 个字符")
    private String title;

    @Schema(description = "问题描述")
    private String content;
}
