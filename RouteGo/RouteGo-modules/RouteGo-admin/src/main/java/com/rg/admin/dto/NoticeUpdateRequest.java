package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "编辑公告入参")
@Data
public class NoticeUpdateRequest {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "类型：1 通知，2 公告")
    private Byte type;

    @Schema(description = "是否置顶：0 否，1 是")
    private Byte isTop;
}
