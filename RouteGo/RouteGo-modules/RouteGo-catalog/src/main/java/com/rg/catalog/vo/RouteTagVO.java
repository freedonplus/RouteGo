package com.rg.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线路标签视图对象。
 */
@Schema(description = "线路标签")
@Data
public class RouteTagVO {

    private Long id;
    private String tagName;
    private String color;
    private Integer sort;
    private Byte status;
    private LocalDateTime createTime;
}
