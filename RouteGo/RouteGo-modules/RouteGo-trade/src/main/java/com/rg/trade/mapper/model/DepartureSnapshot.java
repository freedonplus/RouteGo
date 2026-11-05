package com.rg.trade.mapper.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 下单时需要从 catalog 域读取的班期快照（只读）。
 */
@Data
public class DepartureSnapshot {

    private Long id;
    private Long routeId;
    private LocalDate departDate;
    private BigDecimal adultPrice;
    private BigDecimal childPrice;
    private Integer stock;
    private Integer sold;
    private Byte status;

    /**
     * 剩余库存。
     */
    public Integer getRemaining() {
        if (stock == null) {
            return 0;
        }
        return stock - (sold == null ? 0 : sold);
    }
}
