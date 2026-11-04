package com.rg.admin.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "公告分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeQuery extends PageQuery {

    @Schema(description = "标题，模糊匹配")
    private String title;

    @Schema(description = "类型：1 通知，2 公告")
    private Byte type;

    @Schema(description = "状态：0 下架，1 发布")
    private Byte status;
}
