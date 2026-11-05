package com.rg.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.catalog.entity.RouteItinerary;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 行程明细 Mapper。
 * <p>
 * {@code uk_route_day(route_id, day_no)} 不含逻辑删除列，逻辑删除后的行仍会占用
 * 「第几天」这个唯一位；行程属于结构性数据，整体覆盖时直接物理删除再加新行。
 */
public interface RouteItineraryMapper extends BaseMapper<RouteItinerary> {

    @Delete("DELETE FROM route_itinerary WHERE route_id = #{routeId}")
    int deletePhysicallyByRouteId(@Param("routeId") Long routeId);

    @Delete("DELETE FROM route_itinerary WHERE id = #{id}")
    int deletePhysicallyById(@Param("id") Long id);
}
