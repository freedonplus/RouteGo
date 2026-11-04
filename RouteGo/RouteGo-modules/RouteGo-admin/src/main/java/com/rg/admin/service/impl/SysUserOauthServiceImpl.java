package com.rg.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.admin.dto.UserOauthBindRequest;
import com.rg.admin.entity.SysRole;
import com.rg.admin.entity.SysUser;
import com.rg.admin.entity.SysUserOauth;
import com.rg.admin.entity.SysUserRole;
import com.rg.admin.mapper.SysRoleMapper;
import com.rg.admin.mapper.SysUserMapper;
import com.rg.admin.mapper.SysUserOauthMapper;
import com.rg.admin.mapper.SysUserRoleMapper;
import com.rg.admin.service.SysUserOauthService;
import com.rg.admin.vo.SysUserOauthVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class SysUserOauthServiceImpl extends ServiceImpl<SysUserOauthMapper, SysUserOauth>
        implements SysUserOauthService {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Set<String> PROVIDERS = Set.of("wechat", "qq", "apple", "dingtalk");
    private static final Set<String> PROTECTED_ROLE_CODES = Set.of("admin", "super_admin");

    private final SysUserOauthMapper oauthMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final CacheHelper cacheHelper;

    public SysUserOauthServiceImpl(SysUserOauthMapper oauthMapper,
                                   SysUserMapper userMapper,
                                   SysUserRoleMapper userRoleMapper,
                                   SysRoleMapper roleMapper,
                                   CacheHelper cacheHelper) {
        this.oauthMapper = oauthMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.cacheHelper = cacheHelper;
    }

    @Override
    public List<SysUserOauthVO> listByUserId(Long userId) {
        return cacheHelper.getOrLoad(
                String.format(CacheKeyConstants.Admin.OAUTH_BY_USER, userId),
                TTL,
                () -> oauthMapper.selectList(Wrappers.<SysUserOauth>lambdaQuery()
                        .eq(SysUserOauth::getUserId, userId)).stream().map(this::toVO).toList());
    }

    @Override
    @Transactional
    public Long bind(UserOauthBindRequest request) {
        if (!PROVIDERS.contains(request.getProvider())) {
            throw new AdminException("不支持的第三方平台");
        }
        requireUser(request.getUserId());
        SysUserOauth oauth = new SysUserOauth();
        oauth.setUserId(request.getUserId());
        oauth.setProvider(request.getProvider());
        oauth.setOpenid(request.getOpenid());
        oauth.setUnionid(request.getUnionid());
        oauth.setNickname(request.getNickname());
        oauth.setAvatar(request.getAvatar());
        oauth.setBindTime(LocalDateTime.now());
        try {
            oauthMapper.insert(oauth);
        } catch (DuplicateKeyException e) {
            throw new AdminException(ErrorCode.CONFLICT, "该第三方账号已绑定");
        }
        cacheHelper.evictAfterCommit(
                String.format(CacheKeyConstants.Admin.OAUTH_BY_USER, request.getUserId()));
        return oauth.getId();
    }

    @Override
    @Transactional
    public void unbind(Long id) {
        SysUserOauth oauth = oauthMapper.selectById(id);
        if (oauth == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "绑定记录不存在");
        }
        SysUser user = requireUser(oauth.getUserId());
        if (!StringUtils.hasText(user.getPassword())) {
            throw new AdminException(ErrorCode.CONFLICT, "用户没有密码登录方式，不能解绑");
        }
        oauthMapper.deleteById(id);
        cacheHelper.evictAfterCommit(
                String.format(CacheKeyConstants.Admin.OAUTH_BY_USER, oauth.getUserId()));
    }

    private SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (hasProtectedRole(userId) && !actorHasProtectedRole()) {
            throw new AdminException(ErrorCode.FORBIDDEN, "无权操作超级管理员的第三方绑定");
        }
        return user;
    }

    private boolean hasProtectedRole(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        return !roleIds.isEmpty() && roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getRoleCode)
                .anyMatch(code -> code != null && PROTECTED_ROLE_CODES.contains(code));
    }

    private boolean actorHasProtectedRole() {
        try {
            return StpUtil.getRoleList().stream()
                    .anyMatch(code -> code != null && PROTECTED_ROLE_CODES.contains(code));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private SysUserOauthVO toVO(SysUserOauth oauth) {
        SysUserOauthVO vo = new SysUserOauthVO();
        vo.setId(oauth.getId());
        vo.setUserId(oauth.getUserId());
        vo.setProvider(oauth.getProvider());
        vo.setOpenid(oauth.getOpenid());
        vo.setUnionid(oauth.getUnionid());
        vo.setNickname(oauth.getNickname());
        vo.setAvatar(oauth.getAvatar());
        vo.setBindTime(oauth.getBindTime());
        return vo;
    }
}
