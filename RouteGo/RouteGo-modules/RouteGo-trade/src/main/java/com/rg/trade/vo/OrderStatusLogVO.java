package com.rg.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单状态流水视图对象。
 */
@Schema(description = "订单状态流水")
@Data
public class OrderStatusLogVO {

    private Long id;

    private Long orderId;

    private Byte fromStatus;

    @Schema(description = "原状态描述；首条流水为空")
    private String fromStatusDesc;

    private Byte toStatus;

    @Schema(description = "新状态描述")
    private String toStatusDesc;

    private Long operatorId;

    private String operatorName;

    private String remark;

    private LocalDateTime createTime;
}
