package com.rg.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 线路分类新增/编辑入参。
 */
@Schema(description = "线路分类新增/编辑入参")
@Data
public class RouteCategorySaveRequest {

    @Schema(description = "父级 ID，顶级传 0", example = "0")
    private Long parentId;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入分类名称")
    @Size(max = 50, message = "分类名称最长 50 个字符")
    private String name;

    @Schema(description = "排序，越小越靠前")
    @Min(value = 0, message = "排序不能为负数")
    private Integer sort;

    @Schema(description = "状态：0 禁用，1 启用")
    private Byte status;

    @Schema(description = "图标")
    @Size(max = 255, message = "图标地址最长 255 个字符")
    private String icon;
}
