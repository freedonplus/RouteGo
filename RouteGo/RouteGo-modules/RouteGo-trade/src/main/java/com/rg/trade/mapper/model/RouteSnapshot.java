package com.rg.trade.mapper.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 下单时需要从 catalog 域读取的线路快照（只读）。
 */
@Data
public class RouteSnapshot {

    private Long id;
    private String title;
    private BigDecimal minPrice;
    private String departureCity;
    private Byte status;
}
