package com.rg.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * C 端发起客服咨询入参。
 * <p>
 * 会员 ID 由后端从登录态注入，客户端不传也不能指定处理人。首条消息允许只发图片，
 * 因此 {@code content} 与 {@code images} 的必填性由 Service 联合校验。
 */
@Schema(description = "C 端发起客服咨询")
@Data
public class CsCustomerTicketCreateRequest {

    @Schema(description = "类型：1 售前，2 订单，3 退款，4 投诉，5 其他", example = "1")
    private Byte type;

    @Schema(description = "标题，留空时自动截取问题描述前 30 字")
    @Size(max = 100, message = "标题最多 100 个字符")
    private String title;

    @Schema(description = "问题描述（可与图片二选一）")
    @Size(max = 2000, message = "问题描述最多 2000 个字符")
    private String content;

    @Schema(description = "图片地址列表，最多 9 张")
    @Size(max = 9, message = "单条消息最多 9 张图片")
    private List<String> images;

    @Schema(description = "关联订单 ID，选填")
    private Long orderId;
}
