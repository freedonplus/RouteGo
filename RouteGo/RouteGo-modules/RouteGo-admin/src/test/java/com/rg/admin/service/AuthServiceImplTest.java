package com.rg.admin.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rg.admin.dto.LoginRequest;
import com.rg.admin.entity.SysUser;
import com.rg.admin.mapper.SysUserMapper;
import com.rg.admin.service.impl.AuthServiceImpl;
import com.rg.admin.vo.LoginResult;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.redis.RedisService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CaptchaService captchaService;
    @Mock
    private RedisService redisService;
    @Mock
    private SysLoginLogService loginLogService;

    private AuthServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysUser.class);
    }

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userMapper, passwordEncoder, captchaService, redisService, loginLogService);
    }

    @Test
    void rejectsWrongCaptchaBeforePasswordCheck() {
        doThrow(new AdminException(400, "验证码错误"))
                .when(captchaService).verify("u", "zzzz");

        assertThatThrownBy(() -> service.login(request("alice", "pwd", "u", "zzzz"), "1.1.1.1"))
                .isInstanceOf(AdminException.class);

        verifyNoInteractions(passwordEncoder, userMapper);
    }

    @Test
    void rejectsWhenLocked() {
        when(redisService.hasKey("admin:Auth:loginLock:alice")).thenReturn(true);

        assertThatThrownBy(() -> service.login(request("alice", "pwd", "u", "ab3d"), "1.1.1.1"))
                .isInstanceOf(AdminException.class)
                .hasMessageContaining("失败次数过多");

        verify(userMapper, never()).selectOne(any());
    }

    @Test
    void dummyHashRunsWhenUserMissing() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.matches("pwd", AuthServiceImpl.DUMMY_HASH)).thenReturn(false);

        assertThatThrownBy(() -> service.login(request("ghost", "pwd", "u", "ab3d"), "1.1.1.1"))
                .isInstanceOf(AdminException.class);

        verify(passwordEncoder).matches("pwd", AuthServiceImpl.DUMMY_HASH);
        verify(loginLogService).record("ghost", "1.1.1.1", false, "账号或密码错误");
    }

    @Test
    void locksAfterFiveFailures() {
        when(userMapper.selectOne(any())).thenReturn(user(9L, "alice", "hash", (byte) 1));
        when(passwordEncoder.matches("pwd", "hash")).thenReturn(false);
        when(redisService.increment("admin:Auth:loginFail:alice", 1))
                .thenReturn(1L, 2L, 3L, 4L, 5L);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.login(request("alice", "pwd", "u", "ab3d"), "1.1.1.1"))
                    .isInstanceOf(AdminException.class);
        }

        verify(redisService).set("admin:Auth:loginLock:alice", "1", Duration.ofMinutes(10));
    }

    @Test
    void successClearsFailureKeysAndWritesLoginLog() {
        SysUser user = user(9L, "alice", "hash", (byte) 1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("pwd", "hash")).thenReturn(true);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            SaTokenInfo tokenInfo = new SaTokenInfo();
            tokenInfo.setTokenName("Authorization");
            tokenInfo.setTokenValue("token-value");
            stp.when(StpUtil::getTokenInfo).thenReturn(tokenInfo);
            SaSession session = mock(SaSession.class);
            stp.when(StpUtil::getSession).thenReturn(session);

            LoginResult result = service.login(request("alice", "pwd", "u", "ab3d"), "1.1.1.1");

            assertThat(result.getTokenValue()).isEqualTo("token-value");
            assertThat(result.getUsername()).isEqualTo("alice");
            stp.verify(() -> StpUtil.login(9L));
            verify(session).set("username", "alice");
        }

        verify(redisService).delete(List.of(
                "admin:Auth:loginFail:alice", "admin:Auth:loginLock:alice"));
        verify(loginLogService).record("alice", "1.1.1.1", true, "登录成功");
    }

    @Test
    void disabledUserIsRejected() {
        when(userMapper.selectOne(any())).thenReturn(user(9L, "alice", "hash", (byte) 0));
        when(passwordEncoder.matches("pwd", "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login(request("alice", "pwd", "u", "ab3d"), "1.1.1.1"))
                .isInstanceOf(AdminException.class)
                .hasMessageContaining("禁用");

        verify(loginLogService).record("alice", "1.1.1.1", false, "账号已被禁用");
    }

    @Test
    void changePasswordReadsHashFromDb() {
        SysUser user = user(9L, "alice", "hash", (byte) 1);
        user.setVersion(3);
        when(userMapper.selectById(9L)).thenReturn(user);
        when(passwordEncoder.matches("old", "hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123")).thenReturn("new-hash");
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);

            service.changePassword("old", "NewPass123");

            stp.verify(() -> StpUtil.logout(9L));
        }

        verify(userMapper).selectById(9L);
        verify(userMapper).updateById(any(SysUser.class));
    }

    @Test
    void changePasswordRejectsWeakPassword() {
        SysUser user = user(9L, "alice", "hash", (byte) 1);
        when(userMapper.selectById(9L)).thenReturn(user);
        when(passwordEncoder.matches("old", "hash")).thenReturn(true);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);

            assertThatThrownBy(() -> service.changePassword("old", "12345678"))
                    .isInstanceOf(AdminException.class)
                    .hasMessageContaining("密码");
        }
    }

    private LoginRequest request(String username, String password, String uuid, String code) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setCaptchaUuid(uuid);
        request.setCaptchaCode(code);
        return request;
    }

    private SysUser user(Long id, String username, String password, byte status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname("昵称");
        user.setStatus(status);
        return user;
    }
}
