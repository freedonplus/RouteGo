package com.rg.admin.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "字典类型分页查询条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class DictTypeQuery extends PageQuery {

    @Schema(description = "字典名称，模糊匹配")
    private String dictName;

    @Schema(description = "字典类型编码，模糊匹配")
    private String dictType;

    @Schema(description = "状态：0 禁用，1 启用")
    private Byte status;
}
