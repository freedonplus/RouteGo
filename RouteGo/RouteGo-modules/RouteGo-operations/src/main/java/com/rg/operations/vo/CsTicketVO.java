package com.rg.operations.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单视图对象。
 * <p>
 * 客户手机号出口脱敏；关联对象（会员、订单、处理人）可能已被删除，
 * 此时只回传 ID，对应名称留空，前端显示「—」即可。
 */
@Schema(description = "客服工单")
@Data
public class CsTicketVO {

    @Schema(description = "工单 ID")
    private Long id;

    @Schema(description = "工单号", example = "TK2026110501732")
    private String ticketNo;

    @Schema(description = "客户 ID")
    private Long customerId;

    @Schema(description = "客户昵称")
    private String customerNickname;

    @Schema(description = "客户手机号（脱敏）", example = "138****8000")
    private String customerPhone;

    @Schema(description = "关联订单 ID")
    private Long orderId;

    @Schema(description = "关联订单号")
    private String orderNo;

    @Schema(description = "类型：1 售前，2 订单，3 退款，4 投诉，5 其他")
    private Byte type;

    @Schema(description = "类型描述")
    private String typeDesc;

    @Schema(description = "工单标题")
    private String title;

    @Schema(description = "问题描述")
    private String content;

    @Schema(description = "优先级：1 低，2 中，3 高")
    private Byte priority;

    @Schema(description = "优先级描述")
    private String priorityDesc;

    @Schema(description = "状态：0 待处理，1 处理中，2 已解决，3 已关闭")
    private Byte status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "处理人 ID")
    private Long handlerId;

    @Schema(description = "处理人姓名")
    private String handlerName;

    @Schema(description = "完成时间：进入已解决/已关闭时写入")
    private LocalDateTime finishTime;

    @Schema(description = "创建人 ID")
    private Long createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最后更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "消息条数，仅详情返回")
    private Long messageCount;
}
