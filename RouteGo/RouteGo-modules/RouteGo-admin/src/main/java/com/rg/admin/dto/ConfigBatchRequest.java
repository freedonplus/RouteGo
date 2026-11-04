package com.rg.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "批量更新系统参数入参")
@Data
public class ConfigBatchRequest {

    @Schema(description = "参数列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "参数列表不能为空")
    private List<Item> items;

    @Schema(description = "单个参数")
    @Data
    public static class Item {

        @Schema(description = "参数键")
        private String configKey;

        @Schema(description = "参数值")
        private String configValue;
    }
}
