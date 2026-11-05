package com.rg.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.catalog.entity.Route;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 线路主表 Mapper。
 * <p>
 * 线路编号上的唯一索引不区分逻辑删除，因此校验唯一性时必须把已删除的行算进来，
 * 否则回收站里的旧编号会被再次使用，最终在 INSERT 阶段抛出主键冲突。
 */
public interface RouteMapper extends BaseMapper<Route> {

    @Select("SELECT COUNT(*) FROM route WHERE route_no = #{routeNo}")
    long countByRouteNoIncludingDeleted(@Param("routeNo") String routeNo);

    @Select("SELECT COUNT(*) FROM route WHERE route_no = #{routeNo} AND id <> #{id}")
    long countByRouteNoExcludingId(@Param("routeNo") String routeNo, @Param("id") Long id);
}
