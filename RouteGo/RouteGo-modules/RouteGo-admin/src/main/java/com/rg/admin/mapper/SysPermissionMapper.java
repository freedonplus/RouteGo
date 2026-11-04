package com.rg.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.admin.entity.SysPermission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 权限点表 Mapper 接口
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-03
 */
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询用户通过角色获得的启用状态权限码。
     */
    @Select("""
            SELECT DISTINCT p.permission_code
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId}
              AND p.deleted = 0
              AND p.status = 1
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
}
