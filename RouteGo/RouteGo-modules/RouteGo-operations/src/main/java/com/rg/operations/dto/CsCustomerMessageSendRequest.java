package com.rg.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * C 端发送客服消息入参。
 * <p>
 * 发送方固定为「客户」，ID 取登录态。文字与图片至少有一个，只发图片时 content 可以为空。
 */
@Schema(description = "C 端发送客服消息")
@Data
public class CsCustomerMessageSendRequest {

    @Schema(description = "工单 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工单 ID 不能为空")
    private Long ticketId;

    @Schema(description = "消息内容，与图片至少填一个")
    @Size(max = 2000, message = "单条消息最多 2000 个字符")
    private String content;

    @Schema(description = "图片地址列表，最多 9 张")
    @Size(max = 9, message = "单条消息最多 9 张图片")
    private List<String> images;
}
