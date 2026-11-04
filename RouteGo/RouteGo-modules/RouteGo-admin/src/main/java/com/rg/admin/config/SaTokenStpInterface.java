package com.rg.admin.config;

import cn.dev33.satoken.stp.StpInterface;
import com.rg.admin.mapper.SysPermissionMapper;
import com.rg.admin.mapper.SysRoleMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 角色/权限数据源。
 * <p>
 * 角色和权限码来自 sys_user_role / sys_role / sys_role_permission /
 * sys_permission；admin / super_admin 角色直接拥有通配权限 {@code *}。
 */
@Component
public class SaTokenStpInterface implements StpInterface {

    private static final String ADMIN_ROLE = "admin";
    private static final String SUPER_ADMIN_ROLE = "super_admin";

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    public SaTokenStpInterface(SysRoleMapper sysRoleMapper,
                               SysPermissionMapper sysPermissionMapper) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> roles = getRoleList(loginId, loginType);
        if (roles.contains(ADMIN_ROLE) || roles.contains(SUPER_ADMIN_ROLE)) {
            return List.of("*");
        }
        return sysPermissionMapper.selectPermissionCodesByUserId(asUserId(loginId));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return sysRoleMapper.selectRoleCodesByUserId(asUserId(loginId));
    }

    private Long asUserId(Object loginId) {
        return Long.valueOf(String.valueOf(loginId));
    }
}
