package com.rg.operations.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 自动分配试算结果。
 * <p>
 * 规则配置完先试算一次：命中的是哪条规则、会分给谁、按什么策略，
 * 免得配错了只能等真实工单进来才发现。
 */
@Schema(description = "自动分配试算结果")
@Data
public class AssignPreviewVO {

    @Schema(description = "是否命中规则")
    private boolean matched;

    @Schema(description = "命中的规则 ID")
    private Long ruleId;

    @Schema(description = "命中的规则名称")
    private String ruleName;

    @Schema(description = "分配策略：1 轮询，2 当前在处理最少")
    private Byte strategy;

    @Schema(description = "分配策略描述")
    private String strategyDesc;

    @Schema(description = "将分配到的客服 ID")
    private Long handlerId;

    @Schema(description = "将分配到的客服姓名")
    private String handlerName;

    @Schema(description = "说明：未命中或候选客服不可用时给出原因")
    private String message;
}
