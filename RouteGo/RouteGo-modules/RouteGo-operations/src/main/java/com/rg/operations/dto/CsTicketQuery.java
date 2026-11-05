package com.rg.operations.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单分页查询条件。
 * <p>
 * {@code keyword} 同时模糊匹配标题与问题描述；{@code unhandled} 用于「只看没人认领」的
 * 快捷筛选，等价于 handler_id 为空且状态为待处理。
 */
@Schema(description = "客服工单分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class CsTicketQuery extends PageQuery {

    @Schema(description = "工单号，模糊匹配")
    private String ticketNo;

    @Schema(description = "标题或问题描述关键词，模糊匹配")
    private String keyword;

    @Schema(description = "客户 ID")
    private Long customerId;

    @Schema(description = "关联订单 ID")
    private Long orderId;

    @Schema(description = "类型：1 售前，2 订单，3 退款，4 投诉，5 其他")
    private Byte type;

    @Schema(description = "优先级：1 低，2 中，3 高")
    private Byte priority;

    @Schema(description = "状态：0 待处理，1 处理中，2 已解决，3 已关闭")
    private Byte status;

    @Schema(description = "处理人 ID")
    private Long handlerId;

    @Schema(description = "只看未指派且待处理的工单")
    private Boolean unhandled;

    @Schema(description = "创建时间起（含）")
    private LocalDateTime createTimeStart;

    @Schema(description = "创建时间止（含）")
    private LocalDateTime createTimeEnd;
}
