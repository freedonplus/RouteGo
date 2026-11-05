package com.rg.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线路图片视图对象。
 */
@Schema(description = "线路图片")
@Data
public class RouteImageVO {

    private Long id;
    private Long routeId;
    private String imageUrl;
    private Integer sort;
    private LocalDateTime createTime;
}
