package com.rg.trade.mapper;

import com.rg.trade.mapper.model.DepartureSnapshot;
import com.rg.trade.mapper.model.RouteSnapshot;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 交易域对 catalog 域的读模型与库存操作。
 * <p>
 * 模块化单体下各域共用一套库，但 catalog 的写入口只应该有一个：线路与班期的
 * 后台维护走 catalog 自己的 Service。这里只放交易链路必须的两件事：
 * 读下单快照、按条件原子扣减/回补班期库存。
 * <p>
 * 扣减用一条带条件的 UPDATE 完成，靠 {@code stock - sold >= 人数} 谓词保证不超卖，
 * 并发下单时只有一个事务能命中，另一个返回 0 行由上层转成「库存不足」。
 */
public interface CatalogGatewayMapper {

    @Select("""
            SELECT id, title, min_price, departure_city, status
            FROM route
            WHERE id = #{routeId} AND deleted = 0
            """)
    RouteSnapshot selectRoute(@Param("routeId") Long routeId);

    @Select("""
            SELECT id, route_id, depart_date, adult_price, child_price, stock, sold, status
            FROM route_departure
            WHERE id = #{departureId} AND deleted = 0
            """)
    DepartureSnapshot selectDeparture(@Param("departureId") Long departureId);

    /**
     * 占用库存：仅在班期启用且余量足够时生效。
     *
     * @return 1 成功，0 余量不足或班期已停售/删除
     */
    @Update("""
            UPDATE route_departure
            SET sold = sold + #{count}
            WHERE id = #{departureId}
              AND deleted = 0
              AND status = 1
              AND stock - sold >= #{count}
            """)
    int occupyStock(@Param("departureId") Long departureId, @Param("count") int count);

    /**
     * 回补库存：取消、退款时归还名额。用 GREATEST 兜底，避免异常数据把已售扣成负数。
     */
    @Update("""
            UPDATE route_departure
            SET sold = GREATEST(sold - #{count}, 0)
            WHERE id = #{departureId} AND deleted = 0
            """)
    int releaseStock(@Param("departureId") Long departureId, @Param("count") int count);

    /**
     * 支付成功后累加线路销量。
     */
    @Update("""
            UPDATE route
            SET sales = sales + #{count}
            WHERE id = #{routeId} AND deleted = 0
            """)
    int increaseRouteSales(@Param("routeId") Long routeId, @Param("count") int count);

    /**
     * 退款完成后回退线路销量。
     */
    @Update("""
            UPDATE route
            SET sales = GREATEST(sales - #{count}, 0)
            WHERE id = #{routeId} AND deleted = 0
            """)
    int decreaseRouteSales(@Param("routeId") Long routeId, @Param("count") int count);
}
