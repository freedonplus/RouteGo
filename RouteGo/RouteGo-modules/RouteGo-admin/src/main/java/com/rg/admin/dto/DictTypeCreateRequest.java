package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Schema(description = "新增字典类型入参")
@Data
public class DictTypeCreateRequest {

    @Schema(description = "字典名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入字典名称")
    private String dictName;

    @Schema(description = "字典类型编码，创建后不可修改", example = "sys_user_status",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入字典类型编码")
    @Pattern(regexp = "^[a-z][a-z0-9_]{1,49}$", message = "字典类型编码只能是小写字母、数字或下划线")
    private String dictType;

    @Schema(description = "状态：0 禁用，1 启用")
    private Byte status;

    @Schema(description = "备注")
    private String remark;
}
