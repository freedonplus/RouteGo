package com.rg.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.admin.dto.AssignRolesRequest;
import com.rg.admin.dto.ResetPasswordRequest;
import com.rg.admin.dto.UserCreateRequest;
import com.rg.admin.dto.UserQuery;
import com.rg.admin.dto.UserUpdateRequest;
import com.rg.admin.entity.SysRole;
import com.rg.admin.entity.SysUser;
import com.rg.admin.entity.SysUserRole;
import com.rg.admin.mapper.AdminRelationMapper;
import com.rg.admin.mapper.SysRoleMapper;
import com.rg.admin.mapper.SysUserMapper;
import com.rg.admin.mapper.SysUserRoleMapper;
import com.rg.admin.model.SysUserCache;
import com.rg.admin.service.SysUserService;
import com.rg.admin.vo.SysUserDetailVO;
import com.rg.admin.vo.SysUserVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.web.PageResult;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * 系统用户业务实现：唯一性、角色校验、脱敏回写保护、缓存失效与踢下线。
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(10);
    private static final Set<String> PROTECTED_ROLE_CODES = Set.of("admin", "super_admin");

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final AdminRelationMapper relationMapper;
    private final CacheHelper cacheHelper;
    private final PasswordEncoder passwordEncoder;

    public SysUserServiceImpl(SysUserMapper userMapper,
                              SysRoleMapper roleMapper,
                              SysUserRoleMapper userRoleMapper,
                              AdminRelationMapper relationMapper,
                              CacheHelper cacheHelper,
                              PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.relationMapper = relationMapper;
        this.cacheHelper = cacheHelper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<SysUserVO> page(UserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(SysUser::getUsername, query.getUsername());
        }
        if (StringUtils.hasText(query.getNickname())) {
            wrapper.like(SysUser::getNickname, query.getNickname());
        }
        if (StringUtils.hasText(query.getPhone())) {
            wrapper.eq(SysUser::getPhone, query.getPhone());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        if (query.getRoleId() != null) {
            List<Long> userIds = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                            .eq(SysUserRole::getRoleId, query.getRoleId()))
                    .stream().map(SysUserRole::getUserId).distinct().toList();
            if (userIds.isEmpty()) {
                return new PageResult<>(List.of(), 0, query.getPageNum(), query.getPageSize(), 0);
            }
            wrapper.in(SysUser::getId, userIds);
        }
        if (query.getCreateTimeStart() != null) {
            wrapper.ge(SysUser::getCreateTime, query.getCreateTimeStart());
        }
        if (query.getCreateTimeEnd() != null) {
            wrapper.le(SysUser::getCreateTime, query.getCreateTimeEnd());
        }
        wrapper.orderByDesc(SysUser::getCreateTime).orderByDesc(SysUser::getId);

        Page<SysUser> page = userMapper.selectPage(query.toPage(), wrapper);
        List<SysUserVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public SysUserDetailVO detail(Long id) {
        SysUserCache cached = cacheHelper.getOrLoad(
                String.format(CacheKeyConstants.Admin.USER_BY_ID, id),
                USER_CACHE_TTL,
                () -> {
                    SysUser user = userMapper.selectById(id);
                    return user == null ? null : toCache(user);
                });
        if (cached == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        SysUserDetailVO vo = new SysUserDetailVO();
        vo.setId(cached.getId());
        vo.setUsername(cached.getUsername());
        vo.setNickname(cached.getNickname());
        vo.setAvatar(cached.getAvatar());
        vo.setPhone(cached.getPhone());
        vo.setEmail(cached.getEmail());
        vo.setStatus(cached.getStatus());
        vo.setVersion(cached.getVersion());
        vo.setRoleIds(listRoleIds(id));
        return vo;
    }

    @Override
    @Transactional
    public Long create(UserCreateRequest request) {
        AuthServiceImpl.validatePasswordStrength(request.getPassword());
        if (userMapper.countByUsernameIncludingDeleted(request.getUsername()) > 0) {
            throw new AdminException(ErrorCode.CONFLICT, "账号已被使用（可能已删除）");
        }
        List<Long> roleIds = validateRoleIds(request.getRoleIds());

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setAvatar(request.getAvatar());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRemark(request.getRemark());
        user.setStatus((byte) 1);
        userMapper.insert(user);
        replaceRoles(user.getId(), roleIds);
        return user.getId();
    }

    @Override
    @Transactional
    public void update(Long id, UserUpdateRequest request) {
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setVersion(request.getVersion());
        update.setNickname(request.getNickname());
        update.setAvatar(request.getAvatar());
        update.setRemark(request.getRemark());
        if (isPlainValue(request.getPhone())) {
            update.setPhone(request.getPhone());
        }
        if (isPlainValue(request.getEmail())) {
            update.setEmail(request.getEmail());
        }
        if (userMapper.updateById(update) == 0) {
            throw new AdminException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试");
        }
        cacheHelper.evictAfterCommit(String.format(CacheKeyConstants.Admin.USER_BY_ID, id));
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Byte status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new AdminException("状态只能是 0 或 1");
        }
        if (StpUtil.getLoginIdAsLong() == id) {
            throw new AdminException("不能停用当前登录账号");
        }
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (status == 0 && hasProtectedRole(id)) {
            throw new AdminException(ErrorCode.FORBIDDEN, "超级管理员账号受保护");
        }
        userMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .set(SysUser::getStatus, status)
                .eq(SysUser::getId, id));
        cacheHelper.evictAfterCommit(userCacheKeys(id));
        if (status == 0) {
            StpUtil.logout(id);
        }
    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        AuthServiceImpl.validatePasswordStrength(request.getNewPassword());
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (hasProtectedRole(id) && !actorHasProtectedRole()) {
            throw new AdminException(ErrorCode.FORBIDDEN, "无权重置超级管理员密码");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setVersion(existing.getVersion());
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        if (userMapper.updateById(update) == 0) {
            throw new AdminException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试");
        }
        cacheHelper.evictAfterCommit(String.format(CacheKeyConstants.Admin.USER_BY_ID, id));
        StpUtil.logout(id);
    }

    @Override
    @Transactional
    public void assignRoles(Long id, AssignRolesRequest request) {
        if (userMapper.selectById(id) == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (hasProtectedRole(id) && !actorHasProtectedRole()) {
            throw new AdminException(ErrorCode.FORBIDDEN, "无权修改超级管理员角色");
        }
        List<Long> roleIds = validateRoleIds(request.getRoleIds());
        replaceRoles(id, roleIds);
        cacheHelper.evictAfterCommit(userCacheKeys(id));
        StpUtil.logout(id);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (StpUtil.getLoginIdAsLong() == id) {
            throw new AdminException("不能删除当前登录账号");
        }
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (hasProtectedRole(id)) {
            throw new AdminException(ErrorCode.FORBIDDEN, "超级管理员账号受保护");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
        cacheHelper.evictAfterCommit(userCacheKeys(id));
        StpUtil.logout(id);
    }

    @Override
    public List<Long> listRoleIds(Long userId) {
        return userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
    }

    private List<Long> validateRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new AdminException("至少分配一个角色");
        }
        List<Long> distinct = roleIds.stream().distinct().toList();
        List<SysRole> roles = roleMapper.selectBatchIds(distinct);
        boolean invalid = roles.size() != distinct.size()
                || roles.stream().anyMatch(role -> role.getStatus() == null || role.getStatus() == 0);
        if (invalid) {
            throw new AdminException("角色不存在或已禁用");
        }
        boolean requestsProtected = roles.stream()
                .map(SysRole::getRoleCode)
                .anyMatch(code -> code != null && PROTECTED_ROLE_CODES.contains(code));
        if (requestsProtected && !actorHasProtectedRole()) {
            throw new AdminException(ErrorCode.FORBIDDEN, "无权分配内置角色");
        }
        return distinct;
    }

    private boolean actorHasProtectedRole() {
        try {
            return StpUtil.getRoleList().stream().anyMatch(PROTECTED_ROLE_CODES::contains);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }

    private boolean hasProtectedRole(Long userId) {
        List<Long> roleIds = listRoleIds(userId);
        if (roleIds.isEmpty()) {
            return false;
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getRoleCode)
                .anyMatch(code -> code != null && PROTECTED_ROLE_CODES.contains(code));
    }

    private boolean isPlainValue(String value) {
        return StringUtils.hasText(value) && !value.contains("*");
    }

    private String[] userCacheKeys(Long id) {
        return new String[]{
                String.format(CacheKeyConstants.Admin.USER_BY_ID, id),
                String.format(CacheKeyConstants.Admin.USER_PERMISSIONS, id),
                String.format(CacheKeyConstants.Admin.USER_ROLES, id),
                String.format(CacheKeyConstants.Admin.USER_MENU_TREE, id)
        };
    }

    private SysUserCache toCache(SysUser user) {
        SysUserCache cache = new SysUserCache();
        cache.setId(user.getId());
        cache.setUsername(user.getUsername());
        cache.setNickname(user.getNickname());
        cache.setAvatar(user.getAvatar());
        cache.setPhone(user.getPhone());
        cache.setEmail(user.getEmail());
        cache.setStatus(user.getStatus());
        cache.setVersion(user.getVersion());
        cache.setCreateBy(user.getCreateBy());
        cache.setUpdateBy(user.getUpdateBy());
        return cache;
    }

    private SysUserVO toVO(SysUser user) {
        SysUserVO vo = new SysUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setLastLoginIp(user.getLastLoginIp());
        vo.setRemark(user.getRemark());
        vo.setVersion(user.getVersion());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
