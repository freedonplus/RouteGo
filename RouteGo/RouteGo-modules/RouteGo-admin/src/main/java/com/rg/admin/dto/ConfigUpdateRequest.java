package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "编辑系统参数入参；参数键不可修改")
@Data
public class ConfigUpdateRequest {

    @Schema(description = "参数名称")
    private String configName;

    @Schema(description = "参数值")
    private String configValue;

    @Schema(description = "备注")
    private String remark;
}
