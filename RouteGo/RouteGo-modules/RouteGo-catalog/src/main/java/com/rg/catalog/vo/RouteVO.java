package com.rg.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 线路视图对象。
 * <p>
 * 分页列表只填充基础字段与 {@code categoryName}；详情接口额外填充
 * {@code tags / images / itineraries}。
 */
@Schema(description = "线路")
@Data
public class RouteVO {

    private Long id;
    private Long categoryId;
    @Schema(description = "分类名称；仅列表与详情返回")
    private String categoryName;
    private String routeNo;
    private String title;
    private String subtitle;
    private String coverImage;
    private String destination;
    private String departureCity;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String address;
    private Integer days;
    private Integer nights;
    private BigDecimal minPrice;
    private BigDecimal originalPrice;
    private String highlights;
    private String includeItems;
    private String excludeItems;
    private String bookingNotice;
    private Integer sales;
    private BigDecimal rating;
    private Byte status;
    private Integer sort;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Schema(description = "标签列表；仅详情返回")
    private List<RouteTagVO> tags;
    @Schema(description = "图片列表；仅详情返回")
    private List<RouteImageVO> images;
    @Schema(description = "行程明细；仅详情返回")
    private List<RouteItineraryVO> itineraries;
}
