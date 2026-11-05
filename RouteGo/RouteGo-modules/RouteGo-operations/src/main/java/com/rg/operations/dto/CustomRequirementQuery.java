package com.rg.operations.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 定制需求分页查询条件。
 * <p>
 * 联系电话是 AES 密文落库、且没有盲索引列，所以这里不提供手机号查询；
 * 需要按人找单请用联系人姓名 + 目的地组合筛选。
 */
@Schema(description = "定制需求分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomRequirementQuery extends PageQuery {

    @Schema(description = "需求单号，模糊匹配")
    private String requirementNo;

    @Schema(description = "联系人、目的地或偏好关键词，模糊匹配")
    private String keyword;

    @Schema(description = "客户 ID")
    private Long customerId;

    @Schema(description = "状态：0 待跟进，1 已报价，2 已成交，3 已关闭")
    private Byte status;

    @Schema(description = "跟进人 ID")
    private Long handlerId;

    @Schema(description = "期望出发日期起（含）")
    private LocalDate departDateStart;

    @Schema(description = "期望出发日期止（含）")
    private LocalDate departDateEnd;

    @Schema(description = "提交时间起（含）")
    private LocalDateTime createTimeStart;

    @Schema(description = "提交时间止（含）")
    private LocalDateTime createTimeEnd;
}
