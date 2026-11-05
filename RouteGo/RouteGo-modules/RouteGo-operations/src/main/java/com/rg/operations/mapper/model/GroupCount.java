package com.rg.operations.mapper.model;

import lombok.Data;

/**
 * 分组计数，用于看板类统计（按状态、按类型）。
 */
@Data
public class GroupCount {

    /** 分组键：状态值或类型值 */
    private Integer groupKey;

    /** 该分组下的记录数 */
    private Long total;
}
