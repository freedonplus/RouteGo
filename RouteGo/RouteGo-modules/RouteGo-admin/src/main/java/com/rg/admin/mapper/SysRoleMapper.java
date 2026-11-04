package com.rg.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.admin.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 角色表 Mapper 接口
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-03
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询用户拥有的启用状态角色编码。
     */
    @Select("""
            SELECT DISTINCT r.role_code
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND r.deleted = 0
              AND r.status = 1
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
