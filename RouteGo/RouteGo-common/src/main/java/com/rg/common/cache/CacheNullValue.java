package com.rg.common.cache;

import java.io.Serializable;

/**
 * 空值缓存哨兵。
 * <p>
 * 查询结果为空时写入短 TTL 的哨兵值，避免同一个不存在的 key
 * 反复穿透到数据库。Redis 序列化走 Jackson，复杂对象可能无法序列化，
 * 因此实际写入的是 {@link #MARKER} 字符串；读取方通过
 * {@link #isNullMarker(Object)} 识别。
 */
public final class CacheNullValue implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final CacheNullValue INSTANCE = new CacheNullValue();

    /** 写入 Redis 的空值标记（字符串对任何序列化器都安全） */
    public static final String MARKER = "__RG_CACHE_NULL__";

    private CacheNullValue() {
    }

    /** 兼容旧哨兵对象与字符串标记两种读法 */
    public static boolean isNullMarker(Object value) {
        return value instanceof CacheNullValue || MARKER.equals(value);
    }
}
