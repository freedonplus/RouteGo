package com.rg.admin.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.rg.admin.dto.LoginRequest;
import com.rg.admin.entity.SysUser;
import com.rg.admin.service.AuthService;
import com.rg.admin.service.SysUserService;
import com.rg.admin.vo.CurrentUserInfo;
import com.rg.admin.vo.LoginResult;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 基于 sys_user + BCrypt + Sa-Token 的登录实现。
 * <p>
 * 登录成功后 Sa-Token 负责签发和存储 Token；角色/权限码由
 * {@code SaTokenStpInterface} 从 RBAC 表读取。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(SysUserService sysUserService, PasswordEncoder passwordEncoder) {
        this.sysUserService = sysUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResult login(LoginRequest request, String clientIp) {
        String username = request.getUsername().trim();
        SysUser user = sysUserService.lambdaQuery()
                .eq(SysUser::getUsername, username)
                .one();

        if (user == null || !StringUtils.hasText(user.getPassword())
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AdminException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new AdminException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        StpUtil.login(user.getId());

        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        sysUserService.updateById(user);

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return new LoginResult(tokenInfo.getTokenName(), tokenInfo.getTokenValue(),
                user.getId(), user.getUsername(), user.getNickname());
    }

    @Override
    public CurrentUserInfo currentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new AdminException(ErrorCode.UNAUTHORIZED, "用户不存在或已被删除");
        }
        return new CurrentUserInfo(user.getId(), user.getUsername(), user.getNickname(),
                StpUtil.getRoleList(), StpUtil.getPermissionList());
    }
}
