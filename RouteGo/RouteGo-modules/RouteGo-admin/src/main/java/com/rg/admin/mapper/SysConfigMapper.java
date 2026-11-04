package com.rg.admin.mapper;

import com.rg.admin.entity.SysConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 系统参数表 Mapper 接口
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-03
 */
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    @Select("SELECT COUNT(1) FROM sys_config WHERE config_key = #{configKey}")
    long countByConfigKeyIncludingDeleted(@Param("configKey") String configKey);

}
