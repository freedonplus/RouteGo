package com.rg.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.catalog.entity.RouteDeparture;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * 出发班期 Mapper。同一线路同一天只能有一个班期，唯一索引不区分逻辑删除，
 * 因此删除班期使用物理删除（已售出的班期由 Service 拦住，不允许删除）。
 */
public interface RouteDepartureMapper extends BaseMapper<RouteDeparture> {

    @Delete("DELETE FROM route_departure WHERE id = #{id}")
    int deletePhysicallyById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM route_departure WHERE route_id = #{routeId} AND depart_date = #{departDate}")
    long countByRouteAndDateIncludingDeleted(@Param("routeId") Long routeId,
                                             @Param("departDate") LocalDate departDate);

    @Select("SELECT COUNT(*) FROM route_departure WHERE route_id = #{routeId} AND depart_date = #{departDate} AND id <> #{id}")
    long countByRouteAndDateExcludingId(@Param("routeId") Long routeId,
                                        @Param("departDate") LocalDate departDate,
                                        @Param("id") Long id);
}
