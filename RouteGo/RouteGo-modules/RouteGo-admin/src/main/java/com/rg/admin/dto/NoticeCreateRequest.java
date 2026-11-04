package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "新增公告入参")
@Data
public class NoticeCreateRequest {

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "类型：1 通知，2 公告")
    private Byte type;

    @Schema(description = "状态：0 下架，1 发布")
    private Byte status;

    @Schema(description = "是否置顶：0 否，1 是")
    private Byte isTop;
}
