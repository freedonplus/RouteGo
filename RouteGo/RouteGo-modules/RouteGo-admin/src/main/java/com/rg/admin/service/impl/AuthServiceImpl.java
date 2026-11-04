package com.rg.admin.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rg.admin.dto.LoginRequest;
import com.rg.admin.entity.SysUser;
import com.rg.admin.mapper.SysUserMapper;
import com.rg.admin.service.AuthService;
import com.rg.admin.service.CaptchaService;
import com.rg.admin.service.SysLoginLogService;
import com.rg.admin.vo.CurrentUserInfo;
import com.rg.admin.vo.LoginResult;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.redis.RedisService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 基于 sys_user + BCrypt + Sa-Token 的登录实现。
 * <p>
 * 登录流程：验证码 → 失败锁定 → 账号密码 → 账号状态；登录成功/失败都会
 * 异步写 sys_login_log。密码校验一律直接查库，不使用用户详情缓存。
 */
@Service
public class AuthServiceImpl implements AuthService {

    public static final String DUMMY_HASH = new BCryptPasswordEncoder().encode("routego-dummy-password");

    private static final int MAX_FAILURES = 5;
    private static final Duration FAILURE_TTL = Duration.ofMinutes(10);
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);
    private static final Pattern PASSWORD_STRENGTH = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)\\S{8,20}$");

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final RedisService redisService;
    private final SysLoginLogService loginLogService;

    public AuthServiceImpl(SysUserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           CaptchaService captchaService,
                           RedisService redisService,
                           SysLoginLogService loginLogService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.captchaService = captchaService;
        this.redisService = redisService;
        this.loginLogService = loginLogService;
    }

    @Override
    public LoginResult login(LoginRequest request, String clientIp) {
        captchaService.verify(request.getCaptchaUuid(), request.getCaptchaCode());

        String username = request.getUsername().trim();
        String failKey = String.format(CacheKeyConstants.Admin.LOGIN_FAIL, username);
        String lockKey = String.format(CacheKeyConstants.Admin.LOGIN_LOCK, username);
        if (Boolean.TRUE.equals(redisService.hasKey(lockKey))) {
            throw new AdminException(ErrorCode.TOO_MANY_REQUESTS, "登录失败次数过多，请 10 分钟后再试");
        }

        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
        if (user == null || !StringUtils.hasText(user.getPassword())) {
            passwordEncoder.matches(request.getPassword(), DUMMY_HASH);
            loginLogService.record(username, clientIp, false, "账号或密码错误");
            throw new AdminException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFailure(failKey, lockKey);
            loginLogService.record(username, clientIp, false, "账号或密码错误");
            throw new AdminException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            loginLogService.record(username, clientIp, false, "账号已被禁用");
            throw new AdminException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        StpUtil.login(user.getId());
        StpUtil.getSession().set("username", user.getUsername());

        userMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .set(SysUser::getLastLoginTime, LocalDateTime.now())
                .set(SysUser::getLastLoginIp, clientIp)
                .eq(SysUser::getId, user.getId()));

        redisService.delete(List.of(failKey, lockKey));
        loginLogService.record(username, clientIp, true, "登录成功");

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return new LoginResult(tokenInfo.getTokenName(), tokenInfo.getTokenValue(),
                user.getId(), user.getUsername(), user.getNickname());
    }

    @Override
    public CurrentUserInfo currentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new AdminException(ErrorCode.UNAUTHORIZED, "用户不存在或已被删除");
        }
        return new CurrentUserInfo(user.getId(), user.getUsername(), user.getNickname(),
                StpUtil.getRoleList(), StpUtil.getPermissionList());
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new AdminException(ErrorCode.UNAUTHORIZED, "用户不存在或已被删除");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new AdminException("旧密码错误");
        }
        validatePasswordStrength(newPassword);

        SysUser update = new SysUser();
        update.setId(userId);
        update.setVersion(user.getVersion());
        update.setPassword(passwordEncoder.encode(newPassword));
        if (userMapper.updateById(update) == 0) {
            throw new AdminException(ErrorCode.CONFLICT, "密码已被修改，请刷新后重试");
        }
        StpUtil.logout(userId);
    }

    private void recordFailure(String failKey, String lockKey) {
        Long count = redisService.increment(failKey, 1);
        if (count != null && count == 1L) {
            redisService.expire(failKey, FAILURE_TTL);
        }
        if (count != null && count >= MAX_FAILURES) {
            redisService.set(lockKey, "1", LOCK_TTL);
        }
    }

    static void validatePasswordStrength(String password) {
        if (password == null || !PASSWORD_STRENGTH.matcher(password).matches()) {
            throw new AdminException("密码需为 8–20 位，且至少包含字母和数字");
        }
    }
}
