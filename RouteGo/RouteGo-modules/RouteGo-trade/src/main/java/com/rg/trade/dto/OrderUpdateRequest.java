package com.rg.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改订单联系信息。金额、线路与出发日期属于快照，不允许事后改。
 */
@Schema(description = "修改订单联系信息")
@Data
public class OrderUpdateRequest {

    @Schema(description = "联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写联系人")
    @Size(max = 50, message = "联系人姓名最多 50 字")
    private String contactName;

    @Schema(description = "联系电话", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写联系电话")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注最多 255 字")
    private String remark;
}
