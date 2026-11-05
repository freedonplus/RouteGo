package com.rg.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出发班期视图对象。
 */
@Schema(description = "出发班期")
@Data
public class RouteDepartureVO {

    private Long id;
    private Long routeId;
    @Schema(description = "线路标题；列表接口按需填充")
    private String routeTitle;
    private LocalDate departDate;
    private BigDecimal adultPrice;
    private BigDecimal childPrice;
    private Integer stock;
    private Integer sold;
    @Schema(description = "剩余库存 = 库存 - 已售")
    private Integer remaining;
    private Byte status;
    private Integer version;
    private LocalDateTime createTime;
}
