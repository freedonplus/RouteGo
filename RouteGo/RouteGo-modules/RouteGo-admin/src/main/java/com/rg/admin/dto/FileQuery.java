package com.rg.admin.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "文件分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class FileQuery extends PageQuery {

    @Schema(description = "原始文件名，模糊匹配")
    private String originalName;

    @Schema(description = "文件类型")
    private String fileType;
}
