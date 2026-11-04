package com.rg.admin.mapper;

import com.rg.admin.entity.SysDictData;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 字典数据表 Mapper 接口
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-03
 */
public interface SysDictDataMapper extends BaseMapper<SysDictData> {

    @Select("SELECT COUNT(1) FROM sys_dict_data WHERE dict_type = #{dictType} AND dict_value = #{dictValue}")
    long countByTypeAndValueIncludingDeleted(@Param("dictType") String dictType,
                                             @Param("dictValue") String dictValue);

}
