package com.rg.admin.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rg.admin.dto.AssignIdsRequest;
import com.rg.admin.entity.SysPermission;
import com.rg.admin.entity.SysRole;
import com.rg.admin.entity.SysRoleMenu;
import com.rg.admin.entity.SysRolePermission;
import com.rg.admin.entity.SysUserRole;
import com.rg.admin.mapper.AdminRelationMapper;
import com.rg.admin.mapper.SysMenuMapper;
import com.rg.admin.mapper.SysPermissionMapper;
import com.rg.admin.mapper.SysRoleMapper;
import com.rg.admin.mapper.SysRoleMenuMapper;
import com.rg.admin.mapper.SysRolePermissionMapper;
import com.rg.admin.mapper.SysUserRoleMapper;
import com.rg.admin.service.impl.SysRoleServiceImpl;
import com.rg.common.cache.CacheHelper;
import com.rg.common.exception.serviceException.AdminException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysRoleServiceImplTest {

    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysRoleMenuMapper roleMenuMapper;
    @Mock
    private SysRolePermissionMapper rolePermissionMapper;
    @Mock
    private SysMenuMapper menuMapper;
    @Mock
    private SysPermissionMapper permissionMapper;
    @Mock
    private AdminRelationMapper relationMapper;
    @Mock
    private CacheHelper cacheHelper;
    @Mock
    private SysUserService userService;

    private SysRoleServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysRole.class);
        TableInfoHelper.initTableInfo(assistant, SysUserRole.class);
        TableInfoHelper.initTableInfo(assistant, SysRoleMenu.class);
        TableInfoHelper.initTableInfo(assistant, SysRolePermission.class);
    }

    @BeforeEach
    void setUp() {
        service = new SysRoleServiceImpl(roleMapper, userRoleMapper, roleMenuMapper,
                rolePermissionMapper, menuMapper, permissionMapper, relationMapper,
                cacheHelper, userService);
    }

    @Test
    void builtInRoleCannotBeDeletedOrDisabled() {
        when(roleMapper.selectById(1L)).thenReturn(role(1L, "super_admin"));

        assertThatThrownBy(() -> service.deleteRole(1L))
                .isInstanceOfSatisfying(AdminException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403));
        assertThatThrownBy(() -> service.updateStatus(1L, (byte) 0))
                .isInstanceOfSatisfying(AdminException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(403));
    }

    @Test
    void roleWithUsersCannotBeDeleted() {
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "ops"));
        when(relationMapper.countUsersByRoleId(2L)).thenReturn(3L);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);

            assertThatThrownBy(() -> service.deleteRole(2L))
                    .isInstanceOf(AdminException.class)
                    .hasMessageContaining("角色下存在用户");
        }

        verify(roleMapper, org.mockito.Mockito.never()).deleteById(2L);
    }

    @Test
    void assignPermissionsReplacesAndEvictsAffectedUsers() {
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "ops"));
        when(relationMapper.selectUserIdsByRoleId(2L)).thenReturn(List.of(7L, 8L));
        SysPermission p1 = permission(11L);
        SysPermission p2 = permission(12L);
        when(permissionMapper.selectBatchIds(anyList())).thenReturn(List.of(p1, p2));

        AssignIdsRequest request = new AssignIdsRequest();
        request.setIds(List.of(11L, 12L));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getPermissionList).thenReturn(List.of("system:test:11", "system:test:12"));

            service.assignPermissions(2L, request);
        }

        verify(rolePermissionMapper).delete(any());
        verify(rolePermissionMapper, times(2)).insert(any(SysRolePermission.class));
        verify(cacheHelper).evictAfterCommit(
                "admin:SysRole:getPermissions:2",
                "admin:SysUser:getPermissions:7",
                "admin:SysUser:getPermissions:8");
    }

    @Test
    void disablingRoleEvictsUserCachesAndKicksOut() {
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "ops"));
        when(relationMapper.selectUserIdsByRoleId(2L)).thenReturn(List.of(7L, 8L));
        when(roleMapper.updateById(any(SysRole.class))).thenReturn(1);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            service.updateStatus(2L, (byte) 0);

            stp.verify(() -> StpUtil.logout(7L));
            stp.verify(() -> StpUtil.logout(8L));
        }

        verify(cacheHelper).evictAfterCommit(
                "admin:SysRole:getById:2",
                "admin:SysUser:getRoles:7",
                "admin:SysUser:getPermissions:7",
                "admin:SysMenu:getUserTree:7",
                "admin:SysUser:getRoles:8",
                "admin:SysUser:getPermissions:8",
                "admin:SysMenu:getUserTree:8");
    }

    @Test
    void assignPermissionsRejectsCodesActorDoesNotHold() {
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "ops"));
        SysPermission p = permission(11L);
        when(permissionMapper.selectBatchIds(anyList())).thenReturn(List.of(p));

        AssignIdsRequest request = new AssignIdsRequest();
        request.setIds(List.of(11L));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getPermissionList).thenReturn(List.of("system:other:list"));

            assertThatThrownBy(() -> service.assignPermissions(2L, request))
                    .isInstanceOfSatisfying(AdminException.class,
                            ex -> assertThat(ex.getCode()).isEqualTo(403));
        }

        verify(rolePermissionMapper, org.mockito.Mockito.never()).delete(any());
    }

    @Test
    void protectedRolePermissionsRequireProtectedActor() {
        when(roleMapper.selectById(1L)).thenReturn(role(1L, "super_admin"));

        AssignIdsRequest request = new AssignIdsRequest();
        request.setIds(List.of());

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getRoleList).thenReturn(List.of("ops"));

            assertThatThrownBy(() -> service.assignPermissions(1L, request))
                    .isInstanceOfSatisfying(AdminException.class,
                            ex -> assertThat(ex.getCode()).isEqualTo(403));
        }
    }

    private SysRole role(Long id, String code) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(code);
        role.setRoleName(code);
        role.setStatus((byte) 1);
        role.setVersion(0);
        return role;
    }

    private SysPermission permission(Long id) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setPermissionCode("system:test:" + id);
        permission.setStatus((byte) 1);
        return permission;
    }
}
