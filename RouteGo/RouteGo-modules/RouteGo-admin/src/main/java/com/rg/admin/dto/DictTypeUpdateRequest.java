package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "编辑字典类型入参；类型编码不可修改")
@Data
public class DictTypeUpdateRequest {

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "状态：0 禁用，1 启用")
    private Byte status;

    @Schema(description = "备注")
    private String remark;
}
