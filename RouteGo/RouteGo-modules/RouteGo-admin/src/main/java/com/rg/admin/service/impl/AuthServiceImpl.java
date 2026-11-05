package com.rg.admin.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rg.admin.dto.LoginRequest;
import com.rg.admin.dto.RegisterRequest;
import com.rg.admin.entity.SysRole;
import com.rg.admin.entity.SysUser;
import com.rg.admin.entity.SysUserRole;
import com.rg.admin.mapper.SysRoleMapper;
import com.rg.admin.mapper.SysUserMapper;
import com.rg.admin.mapper.SysUserRoleMapper;
import com.rg.admin.service.AuthService;
import com.rg.admin.service.CaptchaService;
import com.rg.admin.service.SysLoginLogService;
import com.rg.admin.service.VerificationCodeService;
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
 * 密码登录流程：图形验证码 → 失败锁定 → 账号密码 → 账号状态；
 * 验证码登录流程：短信/邮箱验证码 → 账号状态。两种方式都会异步写 sys_login_log。
 * <p>
 * 账号统一输入：一个输入框同时接受用户名、手机号、邮箱，按
 * {@code username = ? OR phone = ? OR email = ?} 单条 SQL 匹配，避免先猜类型再查两次。
 */
@Service
public class AuthServiceImpl implements AuthService {

    public static final String DUMMY_HASH = new BCryptPasswordEncoder().encode("routego-dummy-password");

    private static final int MAX_FAILURES = 5;
    private static final Duration FAILURE_TTL = Duration.ofMinutes(10);
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);
    private static final Pattern PASSWORD_STRENGTH = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)\\S{8,20}$");
    /** 注册用户默认绑定的角色编码（sys_role.role_code） */
    public static final String DEFAULT_ROLE_CODE = "normal_user";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final VerificationCodeService verificationCodeService;
    private final RedisService redisService;
    private final SysLoginLogService loginLogService;

    public AuthServiceImpl(SysUserMapper userMapper,
                           SysRoleMapper roleMapper,
                           SysUserRoleMapper userRoleMapper,
                           PasswordEncoder passwordEncoder,
                           CaptchaService captchaService,
                           VerificationCodeService verificationCodeService,
                           RedisService redisService,
                           SysLoginLogService loginLogService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.captchaService = captchaService;
        this.verificationCodeService = verificationCodeService;
        this.redisService = redisService;
        this.loginLogService = loginLogService;
    }

    @Override
    public LoginResult login(LoginRequest request, String clientIp) {
        captchaService.verify(request.getCaptchaUuid(), request.getCaptchaCode());

        String account = request.resolveAccount();
        String failKey = String.format(CacheKeyConstants.Admin.LOGIN_FAIL, account);
        String lockKey = String.format(CacheKeyConstants.Admin.LOGIN_LOCK, account);
        assertNotLocked(lockKey);

        SysUser user = findByAccount(account);
        if (user == null || !StringUtils.hasText(user.getPassword())) {
            passwordEncoder.matches(request.getPassword(), DUMMY_HASH);
            loginLogService.record(account, clientIp, false, "账号或密码错误");
            throw new AdminException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFailure(failKey, lockKey);
            loginLogService.record(account, clientIp, false, "账号或密码错误");
            throw new AdminException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (isDisabled(user)) {
            loginLogService.record(account, clientIp, false, "账号已被禁用");
            throw new AdminException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        return completeLogin(user, failKey, lockKey, clientIp, "登录成功");
    }

    @Override
    public LoginResult loginTrusted(Long userId, String clientIp, String remark) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new AdminException(ErrorCode.UNAUTHORIZED, "用户不存在或已被删除");
        }
        if (isDisabled(user)) {
            throw new AdminException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        String failKey = String.format(CacheKeyConstants.Admin.LOGIN_FAIL, user.getUsername());
        String lockKey = String.format(CacheKeyConstants.Admin.LOGIN_LOCK, user.getUsername());
        return completeLogin(user, failKey, lockKey, clientIp, remark == null ? "登录成功" : remark);
    }

    @Override
    public LoginResult loginByCode(String target, String code, String clientIp) {
        String account = target == null ? null : target.trim();
        String failKey = String.format(CacheKeyConstants.Admin.LOGIN_FAIL, account);
        String lockKey = String.format(CacheKeyConstants.Admin.LOGIN_LOCK, account);
        assertNotLocked(lockKey);

        // 先校验验证码：账号不存在时也照常消耗次数，避免被用来枚举账号
        verificationCodeService.verify(account, VerificationCodeService.Scene.LOGIN, code);

        SysUser user = findByAccount(account);
        if (user == null) {
            loginLogService.record(account, clientIp, false, "账号未绑定");
            throw new AdminException(ErrorCode.UNAUTHORIZED, "该手机号/邮箱未绑定账号，请使用密码登录");
        }
        if (isDisabled(user)) {
            loginLogService.record(account, clientIp, false, "账号已被禁用");
            throw new AdminException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        return completeLogin(user, failKey, lockKey, clientIp, "验证码登录");
    }

    @Override
    public LoginResult register(RegisterRequest request, String clientIp) {
        String phone = request.getPhone().trim();
        // 先校验验证码：手机号已注册时也照常消费验证码，避免被用来探测注册情况
        verificationCodeService.verify(phone, VerificationCodeService.Scene.REGISTER, request.getCode());

        if (findByAccount(phone) != null) {
            throw new AdminException(ErrorCode.CONFLICT, "该手机号已注册，请直接登录");
        }
        validatePasswordStrength(request.getPassword());

        SysRole defaultRole = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, DEFAULT_ROLE_CODE)
                .eq(SysRole::getStatus, 1)
                .eq(SysRole::getDeleted, 0)
                .last("LIMIT 1"));
        if (defaultRole == null) {
            throw new AdminException(ErrorCode.SERVICE_UNAVAILABLE, "默认角色未初始化，请联系管理员");
        }

        SysUser user = new SysUser();
        user.setUsername(phone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname().trim()
                : "用户" + phone.substring(phone.length() - 4));
        user.setPhone(phone);
        user.setStatus((byte) 1);
        user.setRemark("手机号验证码注册，默认绑定普通用户");
        userMapper.insert(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(defaultRole.getId());
        userRoleMapper.insert(userRole);

        return completeLogin(user, null, null, clientIp, "注册登录");
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

    /**
     * 统一账号匹配：用户名 / 手机号 / 邮箱任一命中即可。
     */
    private SysUser findByAccount(String account) {
        if (!StringUtils.hasText(account)) {
            return null;
        }
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.<SysUser>lambdaQuery()
                .and(w -> w.eq(SysUser::getUsername, account)
                        .or().eq(SysUser::getPhone, account)
                        .or().eq(SysUser::getEmail, account))
                .last("LIMIT 1");
        return userMapper.selectOne(wrapper);
    }

    private void assertNotLocked(String lockKey) {
        if (Boolean.TRUE.equals(redisService.hasKey(lockKey))) {
            throw new AdminException(ErrorCode.TOO_MANY_REQUESTS, "登录失败次数过多，请 10 分钟后再试");
        }
    }

    private boolean isDisabled(SysUser user) {
        return user.getStatus() == null || user.getStatus() == 0;
    }

    private LoginResult completeLogin(SysUser user, String failKey, String lockKey,
                                      String clientIp, String remark) {
        StpUtil.login(user.getId());
        StpUtil.getSession().set("username", user.getUsername());

        userMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .set(SysUser::getLastLoginTime, LocalDateTime.now())
                .set(SysUser::getLastLoginIp, clientIp)
                .eq(SysUser::getId, user.getId()));

        redisService.delete(List.of(failKey == null ? "register:no-fail-key" : failKey,
                lockKey == null ? "register:no-lock-key" : lockKey));
        loginLogService.record(user.getUsername(), clientIp, true, remark);

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return new LoginResult(tokenInfo.getTokenName(), tokenInfo.getTokenValue(),
                user.getId(), user.getUsername(), user.getNickname());
    }

    private void recordFailure(String failKey, String lockKey) {
        // INCR 与 EXPIRE 原子执行，避免进程中断留下永不过期的失败计数 key
        Long count = redisService.incrementWithTtl(failKey, 1, FAILURE_TTL);
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
