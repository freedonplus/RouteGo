package com.rg.admin.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * admin 关联表的只读查询，用于规则校验与缓存精确失效。
 */
public interface AdminRelationMapper {

    @Select("""
            SELECT DISTINCT ur.user_id
            FROM sys_user_role ur
            JOIN sys_user u ON u.id = ur.user_id
            WHERE ur.role_id = #{roleId} AND u.deleted = 0
            """)
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    @Select("""
            SELECT DISTINCT ur.user_id
            FROM sys_user_role ur
            JOIN sys_role_permission rp ON rp.role_id = ur.role_id
            JOIN sys_user u ON u.id = ur.user_id
            WHERE rp.permission_id = #{permissionId} AND u.deleted = 0
            """)
    List<Long> selectUserIdsByPermissionId(@Param("permissionId") Long permissionId);

    @Select("""
            SELECT DISTINCT ur.user_id
            FROM sys_user_role ur
            JOIN sys_role_menu rm ON rm.role_id = ur.role_id
            JOIN sys_user u ON u.id = ur.user_id
            WHERE rm.menu_id = #{menuId} AND u.deleted = 0
            """)
    List<Long> selectUserIdsByMenuId(@Param("menuId") Long menuId);

    @Select("""
            SELECT DISTINCT rm.role_id
            FROM sys_role_menu rm
            JOIN sys_role r ON r.id = rm.role_id
            WHERE rm.menu_id = #{menuId} AND r.deleted = 0
            """)
    List<Long> selectRoleIdsByMenuId(@Param("menuId") Long menuId);

    @Select("""
            SELECT DISTINCT rp.role_id
            FROM sys_role_permission rp
            JOIN sys_role r ON r.id = rp.role_id
            WHERE rp.permission_id = #{permissionId} AND r.deleted = 0
            """)
    List<Long> selectRoleIdsByPermissionId(@Param("permissionId") Long permissionId);

    @Select("""
            SELECT COUNT(DISTINCT ur.user_id)
            FROM sys_user_role ur
            JOIN sys_user u ON u.id = ur.user_id
            WHERE ur.role_id = #{roleId} AND u.deleted = 0
            """)
    long countUsersByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量统计多个角色的用户数（一次 GROUP BY 替代逐角色 COUNT，消除列表页 N+1）。
     * 返回的 Map 含 roleId、userCount 两个字段。
     */
    @Select("""
            <script>
            SELECT ur.role_id AS roleId, COUNT(DISTINCT ur.user_id) AS userCount
            FROM sys_user_role ur
            JOIN sys_user u ON u.id = ur.user_id
            WHERE u.deleted = 0
              AND ur.role_id IN
              <foreach collection='roleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            GROUP BY ur.role_id
            </script>
            """)
    List<Map<String, Object>> countUsersByRoleIds(@Param("roleIds") List<Long> roleIds);

    @Select("""
            SELECT COUNT(DISTINCT rp.role_id)
            FROM sys_role_permission rp
            JOIN sys_role r ON r.id = rp.role_id
            WHERE rp.permission_id = #{permissionId} AND r.deleted = 0
            """)
    long countRolesByPermissionId(@Param("permissionId") Long permissionId);

    @Select("""
            SELECT COUNT(DISTINCT rm.role_id)
            FROM sys_role_menu rm
            JOIN sys_role r ON r.id = rm.role_id
            WHERE rm.menu_id = #{menuId} AND r.deleted = 0
            """)
    long countRolesByMenuId(@Param("menuId") Long menuId);

    @Select("""
            SELECT DISTINCT rm.menu_id
            FROM sys_user_role ur
            JOIN sys_role_menu rm ON rm.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
            """)
    List<Long> selectMenuIdsByUserId(@Param("userId") Long userId);
}
