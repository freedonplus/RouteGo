package com.rg.operations.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单消息视图对象。
 */
@Schema(description = "工单消息")
@Data
public class CsMessageVO {

    @Schema(description = "消息 ID")
    private Long id;

    @Schema(description = "工单 ID")
    private Long ticketId;

    @Schema(description = "发送方：1 客户，2 系统用户")
    private Byte senderType;

    @Schema(description = "发送方描述")
    private String senderTypeDesc;

    @Schema(description = "发送方 ID")
    private Long senderId;

    @Schema(description = "发送方姓名")
    private String senderName;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "图片地址列表")
    private List<String> images;

    @Schema(description = "发送时间")
    private LocalDateTime createTime;
}
