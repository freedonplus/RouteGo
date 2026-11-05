package com.rg.operations.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 定制报价视图对象。
 */
@Schema(description = "定制报价")
@Data
public class CustomQuoteVO {

    @Schema(description = "报价 ID")
    private Long id;

    @Schema(description = "需求单 ID")
    private Long requirementId;

    @Schema(description = "需求单号")
    private String requirementNo;

    @Schema(description = "报价单号", example = "CQ2026110503915")
    private String quoteNo;

    @Schema(description = "报价金额（元）")
    private BigDecimal amount;

    @Schema(description = "报价明细")
    private String detail;

    @Schema(description = "有效期至（含）")
    private LocalDate validUntil;

    @Schema(description = "状态：0 待确认，1 已采纳，2 已失效")
    private Byte status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "是否已过有效期（待确认且 validUntil 早于今天）")
    private Boolean expired;

    @Schema(description = "创建人 ID")
    private Long createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
