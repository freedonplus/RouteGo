package com.rg.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评价回复入参。
 */
@Schema(description = "线路评价回复入参")
@Data
public class RouteReviewReplyRequest {

    @Schema(description = "回复内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入回复内容")
    @Size(max = 2000, message = "回复内容最长 2000 个字符")
    private String reply;
}
