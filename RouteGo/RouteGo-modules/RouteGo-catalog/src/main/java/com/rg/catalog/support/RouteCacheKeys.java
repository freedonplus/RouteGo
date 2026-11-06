package com.rg.catalog.support;

import com.rg.common.constant.CacheKeyConstants;

import java.time.Duration;

/**
 * 线路相关缓存 key 与有效期。
 * <p>
 * 只缓存「线路详情」这一类读多写少、结构稳定的聚合结果：
 * <ul>
 *   <li>列表带分页与多组筛选条件，key 空间大、失效面广，短 TTL 省下的那点 DB 时间
 *       抵不上脏数据的风险，因此列表不缓存；</li>
 *   <li>详情缓存里存的是库内的原始相对路径（<code>/covers/xx.png</code>），
 *       签名临时链接、绝对地址都在 Controller 出参时生成，不进缓存；</li>
 *   <li>TTL 必须小于临时链接的有效期（2 小时），否则会出现「缓存里的链接已过期」。</li>
 * </ul>
 */
public final class RouteCacheKeys {

    /** 详情缓存有效期：30 分钟，远小于临时预览链接的 2 小时 */
    public static final Duration DETAIL_TTL = Duration.ofMinutes(30);

    private RouteCacheKeys() {
    }

    public static String detail(Long routeId) {
        return String.format(CacheKeyConstants.Catalog.ROUTE_DETAIL, routeId);
    }
}
