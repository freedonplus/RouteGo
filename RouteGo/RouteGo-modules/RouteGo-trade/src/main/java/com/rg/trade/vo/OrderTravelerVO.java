package com.rg.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 出行人视图对象，证件号与手机号默认脱敏。
 */
@Schema(description = "出行人")
@Data
public class OrderTravelerVO {

    private Long id;

    private Long orderId;

    private String name;

    @Schema(description = "证件号（脱敏）", example = "4301**********1234")
    private String idCard;

    @Schema(description = "手机号（脱敏）", example = "138****8000")
    private String phone;

    private Byte gender;

    @Schema(description = "性别描述")
    private String genderDesc;

    private Byte travelerType;

    @Schema(description = "类型描述", example = "成人")
    private String travelerTypeDesc;

    private String remark;

    private LocalDateTime createTime;
}
