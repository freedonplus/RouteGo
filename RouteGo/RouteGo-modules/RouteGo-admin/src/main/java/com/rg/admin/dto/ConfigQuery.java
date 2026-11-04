package com.rg.admin.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "系统参数分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigQuery extends PageQuery {

    @Schema(description = "参数名称，模糊匹配")
    private String configName;

    @Schema(description = "参数键，模糊匹配")
    private String configKey;
}
