package com.rg.admin.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
