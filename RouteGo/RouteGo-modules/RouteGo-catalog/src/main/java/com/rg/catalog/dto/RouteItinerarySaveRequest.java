package com.rg.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 行程明细新增/编辑入参。
 */
@Schema(description = "行程明细新增/编辑入参")
@Data
public class RouteItinerarySaveRequest {

    @Schema(description = "线路 ID；批量保存时以路径参数为准", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择线路")
    private Long routeId;

    @Schema(description = "第几天，同一条线路内不可重复", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请输入第几天")
    @Min(value = 1, message = "天数从 1 开始")
    @Max(value = 365, message = "天数最多 365")
    private Integer dayNo;

    @Schema(description = "当日主题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入当日主题")
    @Size(max = 100, message = "当日主题最长 100 个字符")
    private String title;

    @Schema(description = "行程描述")
    private String description;

    @Schema(description = "用餐说明")
    @Size(max = 50, message = "用餐说明最长 50 个字符")
    private String meals;

    @Schema(description = "住宿说明")
    @Size(max = 100, message = "住宿说明最长 100 个字符")
    private String hotel;

    @Schema(description = "交通方式")
    @Size(max = 50, message = "交通方式最长 50 个字符")
    private String transport;

    @Schema(description = "配图 URL")
    @Size(max = 255, message = "配图地址最长 255 个字符")
    private String image;

    @Schema(description = "当日地点经度（GCJ-02）")
    private BigDecimal longitude;

    @Schema(description = "当日地点纬度（GCJ-02）")
    private BigDecimal latitude;

    @Schema(description = "当日地点地址")
    @Size(max = 255, message = "地址最长 255 个字符")
    private String address;

    @Schema(description = "排序")
    private Integer sort;
}
