package com.rg.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新建定制需求入参。
 * <p>
 * 联系电话入口即加密，库里只留密文；格式先按大陆手机号校验，海外号码后续放宽规则时
 * 记得同步修改这里与前端表单校验。
 */
@Schema(description = "新建定制需求")
@Data
public class CustomRequirementCreateRequest {

    @Schema(description = "客户 ID，可空（未注册的咨询客户）")
    private Long customerId;

    @Schema(description = "联系人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "联系人不能为空")
    @Size(max = 50, message = "联系人最多 50 个字符")
    private String contactName;

    @Schema(description = "联系电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;

    @Schema(description = "目的地", requiredMode = Schema.RequiredMode.REQUIRED, example = "云南 大理 丽江")
    @NotBlank(message = "目的地不能为空")
    @Size(max = 100, message = "目的地最多 100 个字符")
    private String destination;

    @Schema(description = "期望出发日期")
    private LocalDate departDate;

    @Schema(description = "出行天数")
    @Min(value = 1, message = "出行天数至少 1 天")
    @Max(value = 365, message = "出行天数最多 365 天")
    private Integer days;

    @Schema(description = "出行人数")
    @Min(value = 1, message = "出行人数至少 1 人")
    @Max(value = 999, message = "出行人数最多 999 人")
    private Integer peopleCount;

    @Schema(description = "预算金额（元）")
    @DecimalMin(value = "0.01", message = "预算必须大于 0")
    private BigDecimal budget;

    @Schema(description = "偏好说明：酒店档次、节奏、忌口、必去景点等")
    private String preferences;

    @Schema(description = "跟进人 ID，可空")
    private Long handlerId;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注最多 255 个字符")
    private String remark;
}
