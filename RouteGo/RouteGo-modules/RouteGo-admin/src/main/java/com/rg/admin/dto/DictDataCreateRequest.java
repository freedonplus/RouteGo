package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "新增字典数据入参")
@Data
public class DictDataCreateRequest {

    @Schema(description = "字典类型编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请选择字典类型")
    private String dictType;

    @Schema(description = "字典标签", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入字典标签")
    private String dictLabel;

    @Schema(description = "字典值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入字典值")
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
