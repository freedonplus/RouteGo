package com.rg.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发送工单消息入参。
 * <p>
 * 走后台接口发送的一律记为「客服」方（sender_type = 2），发送人取当前登录账号，
 * 防止前端伪造发送方。
 */
@Schema(description = "发送工单消息")
@Data
public class CsMessageSendRequest {

    @Schema(description = "工单 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工单 ID 不能为空")
    private Long ticketId;

    @Schema(description = "消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "单条消息最多 5000 个字符")
    private String content;

    @Schema(description = "图片地址列表，最多 9 张")
    @Size(max = 9, message = "单条消息最多 9 张图片")
    private List<String> images;
}
