package com.rg.common.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页查询基础入参。
 * <p>
 * 业务 Query 继承它即可获得统一的页码/页大小约束，
 * 通过 {@link #toPage()} 直接交给 MyBatis-Plus。
 */
@Data
public class PageQuery {

    @Min(value = 1, message = "页码最小为 1")
    private long pageNum = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 200, message = "每页条数最大为 200")
    private long pageSize = 10;

    public <T> Page<T> toPage() {
        return new Page<>(pageNum, pageSize);
    }
}
