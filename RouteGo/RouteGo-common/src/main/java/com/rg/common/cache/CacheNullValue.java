package com.rg.common.cache;

import java.io.Serializable;

/**
 * 空值缓存哨兵。
 * <p>
 * 查询结果为空时写入该对象并设置较短的 TTL，避免同一个不存在的 key
 * 反复穿透到数据库。读取方必须先识别它再转成 {@code null}。
 */
public final class CacheNullValue implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final CacheNullValue INSTANCE = new CacheNullValue();

    private CacheNullValue() {
    }
}
