package com.rg.common.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页返回体。
 * <p>
 * 全局约定会把 Long 序列化为字符串（防雪花 ID 精度丢失），但分页的
 * total/pageNum/pageSize/pages 是前端分页组件要求的小整数，必须保持
 * JSON 数值类型，这里用 {@link RawNumberSerializer} 强制按数值输出。
 * <p>
 * 注意不要换回 Jackson 的 {@code NumberSerializer}：它只有带参构造器，
 * 会被 Spring 容器实例化逻辑判定为需要注入 {@code Class<Number>} 而抛
 * {@code UnsatisfiedDependencyException}，导致所有分页接口 500。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records;

    @JsonSerialize(using = RawNumberSerializer.class)
    private long total;

    @JsonSerialize(using = RawNumberSerializer.class)
    private long pageNum;

    @JsonSerialize(using = RawNumberSerializer.class)
    private long pageSize;

    @JsonSerialize(using = RawNumberSerializer.class)
    private long pages;

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(),
                page.getSize(), page.getPages());
    }
}
