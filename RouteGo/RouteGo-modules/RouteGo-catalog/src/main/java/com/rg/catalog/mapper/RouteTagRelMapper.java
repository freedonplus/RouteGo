package com.rg.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.catalog.entity.RouteTagRel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 线路标签关联 Mapper。
 * <p>
 * {@code uk_route_tag(route_id, tag_id)} 不含逻辑删除列，重复绑定前必须物理删除旧关系，
 * 否则唯一索引会挡住重新绑定。
 */
public interface RouteTagRelMapper extends BaseMapper<RouteTagRel> {

    @Delete("DELETE FROM route_tag_rel WHERE route_id = #{routeId}")
    int deletePhysicallyByRouteId(@Param("routeId") Long routeId);

    @Delete("DELETE FROM route_tag_rel WHERE route_id = #{routeId} AND tag_id = #{tagId}")
    int deletePhysically(@Param("routeId") Long routeId, @Param("tagId") Long tagId);
}
