package com.rg.operations.dto;

import com.rg.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自动分配规则分页查询。
 */
@Schema(description = "自动分配规则查询")
@Data
@EqualsAndHashCode(callSuper = true)
public class CsAssignRuleQuery extends PageQuery {

    @Schema(description = "规则名称关键字")
    private String keyword;

    @Schema(description = "状态：0 停用，1 启用；不传查全部")
    private Integer enabled;
}
