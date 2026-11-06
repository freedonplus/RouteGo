package com.rg.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 自动分配规则的新建/编辑入参。
 * <p>
 * {@code matchType} / {@code matchPriority} 允许为 null，表示该维度不限；
 * 两个都不填即为兜底规则（所有没人指派的工单都会进这里）。
 */
@Schema(description = "自动分配规则保存入参")
@Data
public class CsAssignRuleSaveRequest {

    @Schema(description = "规则名称", example = "售前咨询-售前组轮询")
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 50, message = "规则名称最多 50 字")
    private String ruleName;

    @Schema(description = "适用工单类型：1 售前，2 订单，3 退款，4 投诉，5 其他；不填不限")
    @Min(value = 1, message = "工单类型取值 1-5")
    @Max(value = 5, message = "工单类型取值 1-5")
    private Integer matchType;

    @Schema(description = "适用优先级：1 低，2 中，3 高；不填不限")
    @Min(value = 1, message = "优先级取值 1-3")
    @Max(value = 3, message = "优先级取值 1-3")
    private Integer matchPriority;

    @Schema(description = "分配策略：1 轮询，2 当前在处理最少", example = "2")
    @NotNull(message = "请选择分配策略")
    @Min(value = 1, message = "分配策略取值 1-2")
    @Max(value = 2, message = "分配策略取值 1-2")
    private Integer strategy;

    @Schema(description = "候选客服 ID 列表，按顺序参与分配")
    @NotEmpty(message = "至少选择一个客服")
    @Size(max = 50, message = "单条规则最多 50 个客服")
    private List<Long> userIds;

    @Schema(description = "是否启用：0 停用，1 启用；不填默认启用")
    private Integer enabled;

    @Schema(description = "匹配顺序，数字小的先命中；不填排最后")
    private Integer sort;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注最多 255 字")
    private String remark;
}
