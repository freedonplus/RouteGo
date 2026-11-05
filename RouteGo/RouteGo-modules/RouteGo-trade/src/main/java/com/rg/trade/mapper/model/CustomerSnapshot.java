package com.rg.trade.mapper.model;

import lombok.Data;

/**
 * 下单时需要从 customer 域读取的客户快照（只读）。
 */
@Data
public class CustomerSnapshot {

    private Long id;
    private String nickname;
    private String phone;
    private String phoneIndex;
    private Byte status;
}
