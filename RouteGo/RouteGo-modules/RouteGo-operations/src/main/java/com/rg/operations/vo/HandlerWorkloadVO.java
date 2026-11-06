package com.rg.operations.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 客服当前在手工单量。
 * <p>
 * 分配规则表单里展示，配置「在处理最少」策略时能看出谁已经压了一堆单。
 */
@Schema(description = "客服在手工单量")
@Data
public class HandlerWorkloadVO {

    @Schema(description = "客服 ID")
    private Long handlerId;

    @Schema(description = "客服姓名")
    private String handlerName;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "处理中的工单数")
    private Long processing;

    @Schema(description = "是否可用：0 已删除，1 正常")
    private Integer available;
}
