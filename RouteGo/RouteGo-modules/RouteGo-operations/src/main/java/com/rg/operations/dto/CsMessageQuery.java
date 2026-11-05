package com.rg.operations.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单消息分页/列表查询条件。
 */
@Schema(description = "工单消息查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class CsMessageQuery extends PageQuery {

    @Schema(description = "工单 ID")
    private Long ticketId;

    @Schema(description = "发送方：1 客户，2 系统用户")
    private Byte senderType;

    @Schema(description = "发送方 ID")
    private Long senderId;

    @Schema(description = "发送时间起（含）")
    private LocalDateTime createTimeStart;

    @Schema(description = "发送时间止（含）")
    private LocalDateTime createTimeEnd;
}
