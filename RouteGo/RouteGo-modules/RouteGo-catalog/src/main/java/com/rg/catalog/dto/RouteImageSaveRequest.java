package com.rg.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 线路图片新增入参。
 */
@Schema(description = "线路图片新增入参")
@Data
public class RouteImageSaveRequest {

    @Schema(description = "线路 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择线路")
    private Long routeId;

    @Schema(description = "图片 URL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请上传图片")
    @Size(max = 500, message = "图片地址最长 500 个字符")
    private String imageUrl;

    @Schema(description = "排序，越小越靠前")
    private Integer sort;
}
