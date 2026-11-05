package com.rg.operations.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 定制需求视图对象。
 * <p>
 * 联系电话出口只给脱敏值；报价数量与最新报价金额只有详情接口会填。
 */
@Schema(description = "定制需求")
@Data
public class CustomRequirementVO {

    @Schema(description = "需求单 ID")
    private Long id;

    @Schema(description = "需求单号", example = "CR2026110502914")
    private String requirementNo;

    @Schema(description = "客户 ID")
    private Long customerId;

    @Schema(description = "客户昵称")
    private String customerNickname;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话（脱敏）", example = "138****8000")
    private String contactPhone;

    @Schema(description = "目的地")
    private String destination;

    @Schema(description = "期望出发日期")
    private LocalDate departDate;

    @Schema(description = "出行天数")
    private Integer days;

    @Schema(description = "出行人数")
    private Integer peopleCount;

    @Schema(description = "预算金额（元）")
    private BigDecimal budget;

    @Schema(description = "偏好说明")
    private String preferences;

    @Schema(description = "状态：0 待跟进，1 已报价，2 已成交，3 已关闭")
    private Byte status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "跟进人 ID")
    private Long handlerId;

    @Schema(description = "跟进人姓名")
    private String handlerName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "提交时间")
    private LocalDateTime createTime;

    @Schema(description = "最后更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "报价版本数，仅详情返回")
    private Long quoteCount;

    @Schema(description = "最新一版报价金额，仅详情返回")
    private BigDecimal latestQuoteAmount;

    @Schema(description = "最新一版报价状态，仅详情返回")
    private Byte latestQuoteStatus;
}
