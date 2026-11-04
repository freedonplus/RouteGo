package com.rg.admin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rg.admin.entity.SysPermission;
import com.rg.admin.mapper.AdminRelationMapper;
import com.rg.admin.mapper.SysPermissionMapper;
import com.rg.admin.mapper.SysRoleMapper;
import com.rg.admin.mapper.SysRolePermissionMapper;
import com.rg.admin.service.impl.SysPermissionServiceImpl;
import com.rg.common.cache.CacheHelper;
import com.rg.common.exception.serviceException.AdminException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysPermissionServiceImplTest {

    @Mock
    private SysPermissionMapper permissionMapper;
    @Mock
    private SysRolePermissionMapper rolePermissionMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private AdminRelationMapper relationMapper;
    @Mock
    private CacheHelper cacheHelper;

    private SysPermissionServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysPermission.class);
    }

    @BeforeEach
    void setUp() {
        service = new SysPermissionServiceImpl(permissionMapper, rolePermissionMapper,
                roleMapper, relationMapper, cacheHelper);
    }

    @Test
    void permissionDeleteBlockedWhenReferenced() {
        when(permissionMapper.selectById(5L)).thenReturn(permission(5L));
        when(relationMapper.countRolesByPermissionId(5L)).thenReturn(1L);

        assertThatThrownBy(() -> service.deletePermission(5L))
                .isInstanceOf(AdminException.class)
                .hasMessageContaining("角色引用");
    }

    private SysPermission permission(Long id) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setPermissionName("查看");
        permission.setPermissionCode("system:test:list");
        permission.setStatus((byte) 1);
        return permission;
    }
}
