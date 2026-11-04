package com.rg.admin.mapper;

import com.rg.admin.entity.SysDictType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 字典类型表 Mapper 接口
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-03
 */
public interface SysDictTypeMapper extends BaseMapper<SysDictType> {

    @Select("SELECT COUNT(1) FROM sys_dict_type WHERE dict_type = #{dictType}")
    long countByDictTypeIncludingDeleted(@Param("dictType") String dictType);

}
