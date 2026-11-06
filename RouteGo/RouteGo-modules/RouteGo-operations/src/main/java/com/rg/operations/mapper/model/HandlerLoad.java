package com.rg.operations.mapper.model;

import lombok.Data;

/**
 * 客服当前在手工单数。
 * <p>
 * 单独定义一个模型而不是复用 {@link GroupCount}：那个类的分组键是 Integer，
 * handler_id 是 bigint，用户量上来后会溢出。
 */
@Data
public class HandlerLoad {

    private Long handlerId;

    private Long total;
}
