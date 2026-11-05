package com.rg.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.catalog.entity.RouteTag;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 标签表 Mapper。标签名唯一索引不区分逻辑删除，唯一性校验需包含已删除行。
 */
public interface RouteTagMapper extends BaseMapper<RouteTag> {

    @Select("SELECT COUNT(*) FROM route_tag WHERE tag_name = #{tagName}")
    long countByTagNameIncludingDeleted(@Param("tagName") String tagName);

    @Select("SELECT COUNT(*) FROM route_tag WHERE tag_name = #{tagName} AND id <> #{id}")
    long countByTagNameExcludingId(@Param("tagName") String tagName, @Param("id") Long id);
}
