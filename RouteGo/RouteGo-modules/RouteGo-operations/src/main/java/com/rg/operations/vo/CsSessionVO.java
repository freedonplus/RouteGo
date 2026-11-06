package com.rg.operations.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * C 端客服会话视图。
 * <p>
 * 与后台的 {@link CsTicketVO} 刻意分开：客户只需要自己的会话上下文，
 * 不需要客户手机号、优先级、处理人 ID 这类内部字段，少给一份就少一份泄露面。
 */
@Schema(description = "C 端客服会话")
@Data
public class CsSessionVO {

    @Schema(description = "工单 ID")
    private Long id;

    @Schema(description = "工单号")
    private String ticketNo;

    @Schema(description = "类型：1 售前，2 订单，3 退款，4 投诉，5 其他")
    private Byte type;

    @Schema(description = "类型描述")
    private String typeDesc;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "状态：0 待处理，1 处理中，2 已解决，3 已关闭")
    private Byte status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "最后一条消息摘要")
    private String lastMessage;

    @Schema(description = "最后一条消息时间")
    private LocalDateTime lastMessageTime;

    @Schema(description = "未读消息数：客服回复后累加，客户进入会话清零")
    private Integer unread;

    @Schema(description = "接待客服昵称，未指派为空")
    private String handlerName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
