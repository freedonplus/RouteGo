package com.rg.operations.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 定制报价分页查询条件。
 */
@Schema(description = "定制报价分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomQuoteQuery extends PageQuery {

    @Schema(description = "需求单 ID")
    private Long requirementId;

    @Schema(description = "报价单号，模糊匹配")
    private String quoteNo;

    @Schema(description = "状态：0 待确认，1 已采纳，2 已失效")
    private Byte status;

    @Schema(description = "只看未过有效期但已逾期的待确认报价")
    private Boolean expiredOnly;

    @Schema(description = "创建时间起（含）")
    private LocalDateTime createTimeStart;

    @Schema(description = "创建时间止（含）")
    private LocalDateTime createTimeEnd;
}
