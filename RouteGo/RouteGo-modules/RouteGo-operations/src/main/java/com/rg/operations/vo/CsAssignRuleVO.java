package com.rg.operations.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动分配规则视图对象。
 * <p>
 * 候选客服同时返回 ID 与姓名：前端编辑时直接用 ID 回填远程选择器，
 * 列表展示直接用姓名，不必为每一行再查一次用户接口。
 */
@Schema(description = "自动分配规则")
@Data
public class CsAssignRuleVO {

    @Schema(description = "规则 ID")
    private Long id;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "适用工单类型；null 不限")
    private Byte matchType;

    @Schema(description = "适用工单类型描述")
    private String matchTypeDesc;

    @Schema(description = "适用优先级；null 不限")
    private Byte matchPriority;

    @Schema(description = "适用优先级描述")
    private String matchPriorityDesc;

    @Schema(description = "分配策略：1 轮询，2 当前在处理最少")
    private Byte strategy;

    @Schema(description = "分配策略描述")
    private String strategyDesc;

    @Schema(description = "候选客服 ID")
    private List<Long> userIds;

    @Schema(description = "候选客服姓名")
    private List<String> userNames;

    @Schema(description = "是否启用：0 停用，1 启用")
    private Byte enabled;

    @Schema(description = "匹配顺序，数字小的先命中")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
