package com.rg.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 出行人入参。
 */
@Schema(description = "出行人")
@Data
public class OrderTravelerSaveRequest {

    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写出行人姓名")
    @Size(max = 50, message = "姓名最多 50 字")
    private String name;

    @Schema(description = "证件号；服务端加密存储，回显时脱敏")
    @Size(max = 32, message = "证件号最多 32 位")
    private String idCard;

    @Schema(description = "手机号；服务端加密存储，回显时脱敏")
    @Size(max = 20, message = "手机号最多 20 位")
    private String phone;

    @Schema(description = "性别：0 未知，1 男，2 女")
    @Min(value = 0, message = "性别取值 0/1/2")
    @Max(value = 2, message = "性别取值 0/1/2")
    private Byte gender;

    @Schema(description = "类型：1 成人（默认），2 儿童")
    @Min(value = 1, message = "类型取值 1/2")
    @Max(value = 2, message = "类型取值 1/2")
    private Byte travelerType;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注最多 255 字")
    private String remark;
}
