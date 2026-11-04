package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "新增系统参数入参")
@Data
public class ConfigCreateRequest {

    @Schema(description = "参数名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入参数名称")
    private String configName;

    @Schema(description = "参数键，创建后不可修改", example = "site.name",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入参数键")
    private String configKey;

    @Schema(description = "参数值")
    private String configValue;

    @Schema(description = "备注")
    private String remark;
}
