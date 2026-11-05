package com.rg.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 线路新增/编辑入参。
 * <p>
 * 新增与编辑共用：{@code routeNo} 只在新增时生效，编辑时忽略（线路编号创建后不可修改）。
 */
@Schema(description = "线路新增/编辑入参")
@Data
public class RouteSaveRequest {

    @Schema(description = "分类 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择线路分类")
    private Long categoryId;

    @Schema(description = "线路编号；新增时留空自动生成，编辑时忽略", example = "RG20261105001")
    @Size(max = 32, message = "线路编号最长 32 个字符")
    private String routeNo;

    @Schema(description = "线路标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入线路标题")
    @Size(max = 100, message = "线路标题最长 100 个字符")
    private String title;

    @Schema(description = "副标题/卖点")
    @Size(max = 200, message = "副标题最长 200 个字符")
    private String subtitle;

    @Schema(description = "封面图 URL")
    @Size(max = 255, message = "封面图地址最长 255 个字符")
    private String coverImage;

    @Schema(description = "目的地", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入目的地")
    @Size(max = 100, message = "目的地最长 100 个字符")
    private String destination;

    @Schema(description = "出发城市", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入出发城市")
    @Size(max = 50, message = "出发城市最长 50 个字符")
    private String departureCity;

    @Schema(description = "目的地经度（GCJ-02）")
    private BigDecimal longitude;

    @Schema(description = "目的地纬度（GCJ-02）")
    private BigDecimal latitude;

    @Schema(description = "目的地地址")
    @Size(max = 255, message = "地址最长 255 个字符")
    private String address;

    @Schema(description = "行程天数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请输入行程天数")
    @Min(value = 1, message = "行程天数至少 1 天")
    @Max(value = 365, message = "行程天数最多 365 天")
    private Integer days;

    @Schema(description = "住宿晚数，不得超过行程天数")
    @Min(value = 0, message = "住宿晚数不能为负数")
    private Integer nights;

    @Schema(description = "起价", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请输入起价")
    @DecimalMin(value = "0.00", message = "起价不能为负数")
    private BigDecimal minPrice;

    @Schema(description = "原价")
    @DecimalMin(value = "0.00", message = "原价不能为负数")
    private BigDecimal originalPrice;

    @Schema(description = "线路亮点")
    private String highlights;

    @Schema(description = "费用包含")
    private String includeItems;

    @Schema(description = "费用不含")
    private String excludeItems;

    @Schema(description = "预订须知")
    private String bookingNotice;

    @Schema(description = "状态：0 下架，1 上架，默认 0")
    private Byte status;

    @Schema(description = "排序，越小越靠前")
    private Integer sort;
}
