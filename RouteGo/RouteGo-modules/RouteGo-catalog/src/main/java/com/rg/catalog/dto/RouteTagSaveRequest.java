package com.rg.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 标签新增/编辑入参。
 */
@Schema(description = "线路标签新增/编辑入参")
@Data
public class RouteTagSaveRequest {

    @Schema(description = "标签名称，全局唯一", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入标签名称")
    @Size(max = 50, message = "标签名称最长 50 个字符")
    private String tagName;

    @Schema(description = "标签颜色，如 #2f6feb")
    @Size(max = 20, message = "颜色值最长 20 个字符")
    private String color;

    @Schema(description = "排序，越小越靠前")
    private Integer sort;

    @Schema(description = "状态：0 禁用，1 启用")
    private Byte status;
}
