package com.rg.admin.mapper;

import com.rg.admin.entity.SysLoginLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * <p>
 * 登录日志表 Mapper 接口
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-03
 */
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {

    @Select("SELECT COUNT(1) FROM sys_login_log WHERE status = #{status} AND login_time >= #{since}")
    long countByStatusSince(@Param("status") Byte status, @Param("since") LocalDateTime since);

    @Select("""
            SELECT COUNT(1) FROM sys_login_log
            WHERE status = #{status} AND login_time >= #{from} AND login_time < #{to}
            """)
    long countByStatusBetween(@Param("status") Byte status,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);

    @Select("""
            SELECT COUNT(DISTINCT username) FROM sys_login_log
            WHERE status = 1 AND login_time >= #{since}
            """)
    long countDistinctUsersSince(@Param("since") LocalDateTime since);
}
