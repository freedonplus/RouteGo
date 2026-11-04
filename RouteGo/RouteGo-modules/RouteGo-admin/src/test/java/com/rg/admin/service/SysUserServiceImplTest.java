package com.rg.admin.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rg.admin.dto.AssignRolesRequest;
import com.rg.admin.dto.ResetPasswordRequest;
import com.rg.admin.dto.UserCreateRequest;
import com.rg.admin.dto.UserUpdateRequest;
import com.rg.admin.entity.SysRole;
import com.rg.admin.entity.SysUser;
import com.rg.admin.entity.SysUserRole;
import com.rg.admin.mapper.AdminRelationMapper;
import com.rg.admin.mapper.SysRoleMapper;
import com.rg.admin.mapper.SysUserMapper;
import com.rg.admin.mapper.SysUserRoleMapper;
import com.rg.admin.service.impl.SysUserServiceImpl;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private AdminRelationMapper relationMapper;
    @Mock
    private CacheHelper cacheHelper;
    @Mock
    private PasswordEncoder passwordEncoder;

    private SysUserServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysUser.class);
        TableInfoHelper.initTableInfo(assistant, SysUserRole.class);
        TableInfoHelper.initTableInfo(assistant, SysRole.class);
    }

    @BeforeEach
    void setUp() {
        service = new SysUserServiceImpl(userMapper, roleMapper, userRoleMapper,
                relationMapper, cacheHelper, passwordEncoder);
    }

    @Test
    void createRejectsCodeReservedByDeletedRow() {
        when(userMapper.countByUsernameIncludingDeleted("alice")).thenReturn(1L);

        assertThatThrownBy(() -> service.create(createRequest("alice", List.of(1L))))
                .isInstanceOfSatisfying(AdminException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(409));
    }

    @Test
    void createRequiresAtLeastOneRole() {
        when(userMapper.countByUsernameIncludingDeleted("alice")).thenReturn(0L);

        assertThatThrownBy(() -> service.create(createRequest("alice", List.of())))
                .isInstanceOf(AdminException.class)
                .hasMessageContaining("至少分配一个角色");
    }

    @Test
    void maskedPhoneIsNotWrittenBack() {
        SysUser existing = user(1L, "alice", "13812348000", "old@example.com");
        when(userMapper.selectById(1L)).thenReturn(existing);
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setVersion(0);
        request.setPhone("138****8000");
        request.setEmail("a***@example.com");

        service.update(1L, request);

        verify(userMapper).updateById(org.mockito.ArgumentMatchers.<SysUser>argThat(
                update -> update.getPhone() == null && update.getEmail() == null));
    }

    @Test
    void cannotDisableSelf() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            assertThatThrownBy(() -> service.updateStatus(1L, (byte) 0))
                    .isInstanceOf(AdminException.class)
                    .hasMessageContaining("当前登录账号");

            verify(userMapper, never()).selectById(1L);
        }
    }

    @Test
    void cannotDisableSuperAdmin() {
        SysUser target = user(2L, "root", null, null);
        when(userMapper.selectById(2L)).thenReturn(target);
        SysUserRole relation = new SysUserRole();
        relation.setUserId(2L);
        relation.setRoleId(5L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(relation));
        SysRole role = new SysRole();
        role.setId(5L);
        role.setRoleCode("super_admin");
        role.setStatus((byte) 1);
        when(roleMapper.selectBatchIds(anyList())).thenReturn(List.of(role));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);

            assertThatThrownBy(() -> service.updateStatus(2L, (byte) 0))
                    .isInstanceOfSatisfying(AdminException.class,
                            ex -> assertThat(ex.getCode()).isEqualTo(403));
        }
    }

    @Test
    void assignRolesReplacesAndEvictsCachesAndKicksOut() {
        when(userMapper.selectById(9L)).thenReturn(user(9L, "bob", null, null));
        SysRole r1 = new SysRole();
        r1.setId(2L);
        r1.setStatus((byte) 1);
        SysRole r2 = new SysRole();
        r2.setId(3L);
        r2.setStatus((byte) 1);
        when(roleMapper.selectBatchIds(anyList())).thenReturn(List.of(r1, r2));

        AssignRolesRequest request = new AssignRolesRequest();
        request.setRoleIds(List.of(2L, 3L));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            service.assignRoles(9L, request);

            stp.verify(() -> StpUtil.logout(9L));
        }

        verify(userRoleMapper).delete(any());
        verify(userRoleMapper, org.mockito.Mockito.times(2)).insert(any(SysUserRole.class));
        verify(cacheHelper).evictAfterCommit(
                "admin:SysUser:getById:9",
                "admin:SysUser:getPermissions:9",
                "admin:SysUser:getRoles:9",
                "admin:SysMenu:getUserTree:9");
    }

    @Test
    void deleteUserIsLogicalAndCleansRelations() {
        SysUser target = user(3L, "bob", null, null);
        when(userMapper.selectById(3L)).thenReturn(target);
        when(userRoleMapper.selectList(any())).thenReturn(List.of());

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);

            service.deleteUser(3L);

            stp.verify(() -> StpUtil.logout(3L));
        }

        verify(userMapper).deleteById(3L);
        verify(userRoleMapper).delete(any());
    }

    @Test
    void createRejectsWeakPassword() {
        UserCreateRequest request = createRequest("alice", List.of(1L));
        request.setPassword("12345678");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminException.class)
                .hasMessageContaining("密码");
    }

    @Test
    void createRejectsProtectedRoleForNormalActor() {
        when(userMapper.countByUsernameIncludingDeleted("alice")).thenReturn(0L);
        SysRole superAdmin = new SysRole();
        superAdmin.setId(5L);
        superAdmin.setRoleCode("super_admin");
        superAdmin.setStatus((byte) 1);
        when(roleMapper.selectBatchIds(anyList())).thenReturn(List.of(superAdmin));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getRoleList).thenReturn(List.of("ops"));

            assertThatThrownBy(() -> service.create(createRequest("alice", List.of(5L))))
                    .isInstanceOfSatisfying(AdminException.class,
                            ex -> assertThat(ex.getCode()).isEqualTo(403));
        }

        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void assignRolesRequiresExistingUser() {
        when(userMapper.selectById(9L)).thenReturn(null);

        AssignRolesRequest request = new AssignRolesRequest();
        request.setRoleIds(List.of(2L));

        assertThatThrownBy(() -> service.assignRoles(9L, request))
                .isInstanceOfSatisfying(AdminException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(404));
    }

    @Test
    void resetProtectedAccountRequiresProtectedActor() {
        SysUser target = user(2L, "root", null, null);
        when(userMapper.selectById(2L)).thenReturn(target);
        SysUserRole relation = new SysUserRole();
        relation.setUserId(2L);
        relation.setRoleId(5L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(relation));
        SysRole role = new SysRole();
        role.setId(5L);
        role.setRoleCode("super_admin");
        role.setStatus((byte) 1);
        when(roleMapper.selectBatchIds(anyList())).thenReturn(List.of(role));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setNewPassword("NewPass123");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getRoleList).thenReturn(List.of("ops"));

            assertThatThrownBy(() -> service.resetPassword(2L, request))
                    .isInstanceOfSatisfying(AdminException.class,
                            ex -> assertThat(ex.getCode()).isEqualTo(403));
        }

        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void resetPasswordValidatesStrength() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setNewPassword("12345678");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);

            assertThatThrownBy(() -> service.resetPassword(3L, request))
                    .isInstanceOf(AdminException.class)
                    .hasMessageContaining("密码");
        }

        verify(userMapper, never()).selectById(3L);
    }

    private UserCreateRequest createRequest(String username, List<Long> roleIds) {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername(username);
        request.setPassword("Passw0rd!");
        request.setNickname("昵称");
        request.setRoleIds(roleIds);
        return request;
    }

    private SysUser user(Long id, String username, String phone, String email) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus((byte) 1);
        user.setVersion(0);
        return user;
    }
}
