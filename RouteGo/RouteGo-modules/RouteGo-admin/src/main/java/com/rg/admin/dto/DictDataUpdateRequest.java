package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "编辑字典数据入参；所属类型不可修改")
@Data
public class DictDataUpdateRequest {

    @Schema(description = "字典标签")
    private String dictLabel;

    @Schema(description = "字典值")
    private String dictValue;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "样式类名")
    private String cssClass;

    @Schema(description = "是否默认：0 否，1 是")
    private Byte isDefault;

    @Schema(description = "状态：0 禁用，1 启用")
    private Byte status;

    @Schema(description = "备注")
    private String remark;
}
