package com.rg.operations.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 工单看板统计。
 * <p>
 * 供工单列表页顶部的状态卡片使用；{@code typeCounts} 的 key 是类型的中文名，
 * 直接可以拿来渲染图例，前端不必再维护一份映射。
 */
@Schema(description = "工单看板统计")
@Data
public class TicketStatisticsVO {

    @Schema(description = "工单总数")
    private Long total;

    @Schema(description = "待处理")
    private Long pending;

    @Schema(description = "处理中")
    private Long processing;

    @Schema(description = "已解决")
    private Long resolved;

    @Schema(description = "已关闭")
    private Long closed;

    @Schema(description = "未指派且待处理的工单数")
    private Long unhandled;

    @Schema(description = "待处理中的高优先级工单数")
    private Long urgentPending;

    @Schema(description = "按类型统计，key 为类型中文名")
    private Map<String, Long> typeCounts;
}
