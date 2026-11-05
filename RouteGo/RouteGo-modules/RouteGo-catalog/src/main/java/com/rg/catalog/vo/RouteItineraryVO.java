package com.rg.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 行程明细视图对象。
 */
@Schema(description = "行程明细")
@Data
public class RouteItineraryVO {

    private Long id;
    private Long routeId;
    private Integer dayNo;
    private String title;
    private String description;
    private String meals;
    private String hotel;
    private String transport;
    private String image;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String address;
    private Integer sort;
    private LocalDateTime createTime;
}
