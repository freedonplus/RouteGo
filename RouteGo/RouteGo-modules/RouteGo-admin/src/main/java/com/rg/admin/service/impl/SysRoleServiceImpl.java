package com.rg.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.admin.dto.AssignIdsRequest;
import com.rg.admin.dto.RoleCreateRequest;
import com.rg.admin.dto.RoleQuery;
import com.rg.admin.dto.RoleUpdateRequest;
import com.rg.admin.dto.UserQuery;
import com.rg.admin.entity.SysMenu;
import com.rg.admin.entity.SysPermission;
import com.rg.admin.entity.SysRole;
import com.rg.admin.entity.SysRoleMenu;
import com.rg.admin.entity.SysRolePermission;
import com.rg.admin.mapper.AdminRelationMapper;
import com.rg.admin.mapper.SysMenuMapper;
import com.rg.admin.mapper.SysPermissionMapper;
import com.rg.admin.mapper.SysRoleMapper;
import com.rg.admin.mapper.SysRoleMenuMapper;
import com.rg.admin.mapper.SysRolePermissionMapper;
import com.rg.admin.mapper.SysUserRoleMapper;
import com.rg.admin.model.SysRoleCache;
import com.rg.admin.service.SysRoleService;
import com.rg.admin.service.SysUserService;
import com.rg.admin.vo.SysRoleDetailVO;
import com.rg.admin.vo.SysRoleVO;
import com.rg.admin.vo.SysUserVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.web.PageQuery;
import com.rg.common.web.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色业务实现：编码不可改、内置角色保护、授权全量替换与按用户精确失效。
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private static final Duration ROLE_CACHE_TTL = Duration.ofMinutes(10);
    private static final Set<String> PROTECTED_ROLE_CODES = Set.of("admin", "super_admin");

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysMenuMapper menuMapper;
    private final SysPermissionMapper permissionMapper;
    private final AdminRelationMapper relationMapper;
    private final CacheHelper cacheHelper;
    private final SysUserService userService;

    public SysRoleServiceImpl(SysRoleMapper roleMapper,
                              SysUserRoleMapper userRoleMapper,
                              SysRoleMenuMapper roleMenuMapper,
                              SysRolePermissionMapper rolePermissionMapper,
                              SysMenuMapper menuMapper,
                              SysPermissionMapper permissionMapper,
                              AdminRelationMapper relationMapper,
                              CacheHelper cacheHelper,
                              SysUserService userService) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.menuMapper = menuMapper;
        this.permissionMapper = permissionMapper;
        this.relationMapper = relationMapper;
        this.cacheHelper = cacheHelper;
        this.userService = userService;
    }

    @Override
    public PageResult<SysRoleVO> page(RoleQuery query) {
        LambdaQueryWrapper<SysRole> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getRoleName())) {
            wrapper.like(SysRole::getRoleName, query.getRoleName());
        }
        if (StringUtils.hasText(query.getRoleCode())) {
            wrapper.like(SysRole::getRoleCode, query.getRoleCode());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysRole::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysRole::getSort).orderByAsc(SysRole::getId);

        Page<SysRole> page = roleMapper.selectPage(query.toPage(), wrapper);
        Map<Long, Long> userCounts = loadUserCounts(page.getRecords());
        List<SysRoleVO> records = page.getRecords().stream()
                .map(role -> toVO(role, userCounts.getOrDefault(role.getId(), 0L)))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public SysRoleDetailVO detail(Long id) {
        SysRoleCache role = cacheHelper.getOrLoad(
                String.format(CacheKeyConstants.Admin.ROLE_BY_ID, id),
                ROLE_CACHE_TTL,
                () -> {
                    SysRole entity = roleMapper.selectById(id);
                    return entity == null ? null : toCache(entity);
                });
        if (role == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        SysRoleDetailVO vo = new SysRoleDetailVO();
        copy(role, vo);
        vo.setMenuIds(listMenuIds(id));
        vo.setPermissionIds(listPermissionIds(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleCreateRequest request) {
        if (roleMapper.countByRoleCodeIncludingDeleted(request.getRoleCode()) > 0) {
            throw new AdminException(ErrorCode.CONFLICT, "角色编码已被使用（可能已删除）");
        }
        SysRole role = new SysRole();
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setSort(request.getSort() == null ? 0 : request.getSort());
        role.setStatus(request.getStatus() == null ? (byte) 1 : request.getStatus());
        role.setRemark(request.getRemark());
        roleMapper.insert(role);
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RoleUpdateRequest request) {
        SysRole existing = roleMapper.selectById(id);
        if (existing == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        SysRole update = new SysRole();
        update.setId(id);
        update.setVersion(request.getVersion());
        update.setRoleName(request.getRoleName());
        update.setSort(request.getSort());
        update.setRemark(request.getRemark());
        if (roleMapper.updateById(update) == 0) {
            throw new AdminException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试");
        }
        cacheHelper.evictAfterCommit(String.format(CacheKeyConstants.Admin.ROLE_BY_ID, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Byte status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new AdminException("状态只能是 0 或 1");
        }
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if (status == 0 && role.getRoleCode() != null
                && PROTECTED_ROLE_CODES.contains(role.getRoleCode())) {
            throw new AdminException(ErrorCode.FORBIDDEN, "内置角色不可停用");
        }
        SysRole update = new SysRole();
        update.setId(id);
        update.setVersion(role.getVersion());
        update.setStatus(status);
        if (roleMapper.updateById(update) == 0) {
            throw new AdminException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试");
        }

        List<String> keys = new ArrayList<>();
        keys.add(String.format(CacheKeyConstants.Admin.ROLE_BY_ID, id));
        List<Long> userIds = relationMapper.selectUserIdsByRoleId(id);
        if (status == 0) {
            for (Long userId : userIds) {
                keys.add(String.format(CacheKeyConstants.Admin.USER_ROLES, userId));
                keys.add(String.format(CacheKeyConstants.Admin.USER_PERMISSIONS, userId));
                keys.add(String.format(CacheKeyConstants.Admin.USER_MENU_TREE, userId));
            }
        }
        cacheHelper.evictAfterCommit(keys.toArray(String[]::new));
        bumpPermVersion();
        if (status == 0) {
            logoutUsersAfterCommit(userIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if (role.getRoleCode() != null && PROTECTED_ROLE_CODES.contains(role.getRoleCode())) {
            throw new AdminException(ErrorCode.FORBIDDEN, "内置角色不可删除");
        }
        if (relationMapper.countUsersByRoleId(id) > 0) {
            throw new AdminException(ErrorCode.CONFLICT, "角色下存在用户，不能删除");
        }
        List<Long> userIds = relationMapper.selectUserIdsByRoleId(id);
        roleMapper.deleteById(id);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, id));

        List<String> keys = new ArrayList<>();
        keys.add(String.format(CacheKeyConstants.Admin.ROLE_BY_ID, id));
        keys.add(String.format(CacheKeyConstants.Admin.ROLE_MENUS, id));
        keys.add(String.format(CacheKeyConstants.Admin.ROLE_PERMISSIONS, id));
        userIds.forEach(userId -> addUserKeys(keys, userId));
        cacheHelper.evictAfterCommit(keys.toArray(String[]::new));
        bumpPermVersion();
        logoutUsersAfterCommit(userIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long id, AssignIdsRequest request) {
        assertCanModifyProtectedRole(requireRole(id));
        List<Long> menuIds = request.getIds().stream().distinct().toList();
        if (!menuIds.isEmpty() && menuMapper.selectBatchIds(menuIds).size() != menuIds.size()) {
            throw new AdminException("菜单不存在");
        }
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
        if (!menuIds.isEmpty()) {
            List<SysRoleMenu> relations = menuIds.stream().map(menuId -> {
                SysRoleMenu relation = new SysRoleMenu();
                relation.setRoleId(id);
                relation.setMenuId(menuId);
                return relation;
            }).toList();
            // 批量插入，替代循环单条 insert
            roleMenuMapper.insertBatch(relations);
        }

        List<String> keys = new ArrayList<>();
        keys.add(String.format(CacheKeyConstants.Admin.ROLE_MENUS, id));
        List<Long> affectedUserIds = relationMapper.selectUserIdsByRoleId(id);
        affectedUserIds.forEach(userId ->
                keys.add(String.format(CacheKeyConstants.Admin.USER_MENU_TREE, userId)));
        cacheHelper.evictAfterCommit(keys.toArray(String[]::new));
        logoutUsersAfterCommit(affectedUserIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long id, AssignIdsRequest request) {
        assertCanModifyProtectedRole(requireRole(id));
        List<Long> permissionIds = request.getIds().stream().distinct().toList();
        if (!permissionIds.isEmpty()) {
            List<SysPermission> permissions = permissionMapper.selectBatchIds(permissionIds);
            boolean invalid = permissions.size() != permissionIds.size()
                    || permissions.stream().anyMatch(p -> p.getStatus() == null || p.getStatus() == 0);
            if (invalid) {
                throw new AdminException("权限点不存在或已禁用");
            }
            assertCanGrant(permissions.stream().map(SysPermission::getPermissionCode).toList());
        }
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, id));
        if (!permissionIds.isEmpty()) {
            List<SysRolePermission> relations = permissionIds.stream().map(permissionId -> {
                SysRolePermission relation = new SysRolePermission();
                relation.setRoleId(id);
                relation.setPermissionId(permissionId);
                return relation;
            }).toList();
            // 批量插入，替代循环单条 insert
            rolePermissionMapper.insertBatch(relations);
        }

        List<String> keys = new ArrayList<>();
        keys.add(String.format(CacheKeyConstants.Admin.ROLE_PERMISSIONS, id));
        List<Long> affectedUserIds = relationMapper.selectUserIdsByRoleId(id);
        affectedUserIds.forEach(userId ->
                keys.add(String.format(CacheKeyConstants.Admin.USER_PERMISSIONS, userId)));
        cacheHelper.evictAfterCommit(keys.toArray(String[]::new));
        bumpPermVersion();
        logoutUsersAfterCommit(affectedUserIds);
    }

    @Override
    public List<Long> listMenuIds(Long roleId) {
        return roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    @Override
    public List<Long> listPermissionIds(Long roleId) {
        return rolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                        .eq(SysRolePermission::getRoleId, roleId))
                .stream().map(SysRolePermission::getPermissionId).toList();
    }

    @Override
    public PageResult<SysUserVO> pageUsers(Long roleId, PageQuery pageQuery) {
        UserQuery query = new UserQuery();
        query.setPageNum(pageQuery.getPageNum());
        query.setPageSize(pageQuery.getPageSize());
        query.setRoleId(roleId);
        return userService.page(query);
    }

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private void assertCanModifyProtectedRole(SysRole role) {
        if (role.getRoleCode() != null
                && PROTECTED_ROLE_CODES.contains(role.getRoleCode())
                && !actorHasProtectedRole()) {
            throw new AdminException(ErrorCode.FORBIDDEN, "无权修改内置角色授权");
        }
    }

    private void assertCanGrant(List<String> permissionCodes) {
        List<String> actorPermissions;
        try {
            actorPermissions = StpUtil.getPermissionList();
        } catch (RuntimeException e) {
            throw new AdminException(ErrorCode.FORBIDDEN, "无法确认操作人权限");
        }
        if (actorPermissions.contains("*")) {
            return;
        }
        if (!actorPermissions.containsAll(permissionCodes)) {
            throw new AdminException(ErrorCode.FORBIDDEN, "无权授予未持有的权限");
        }
    }

    private boolean actorHasProtectedRole() {
        try {
            return StpUtil.getRoleList().stream()
                    .anyMatch(code -> code != null && PROTECTED_ROLE_CODES.contains(code));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void logoutUsersAfterCommit(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        Runnable action = () -> userIds.forEach(StpUtil::logout);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private void addUserKeys(List<String> keys, Long userId) {
        keys.add(String.format(CacheKeyConstants.Admin.USER_ROLES, userId));
        keys.add(String.format(CacheKeyConstants.Admin.USER_PERMISSIONS, userId));
        keys.add(String.format(CacheKeyConstants.Admin.USER_MENU_TREE, userId));
    }

    /**
     * 权限版本号自增，使所有用户的 Sa-Token 权限缓存 key 立即失效。
     * <p>
     * 不能用 delete：版本号删除后 {@code SaTokenStpInterface} 会回退到 "0"，
     * 与变更前的 key 完全相同，旧权限缓存继续命中。
     */
    private void bumpPermVersion() {
        cacheHelper.incrementAfterCommit(CacheKeyConstants.Admin.PERM_VERSION, 1L);
    }

    private SysRoleVO toVO(SysRole role, long userCount) {
        SysRoleVO vo = new SysRoleVO();
        copy(role, vo);
        vo.setUserCount(userCount);
        return vo;
    }

    /**
     * 一条 GROUP BY 聚合回填各角色用户数，替代每行一次 COUNT 的 N+1 查询。
     */
    private Map<Long, Long> loadUserCounts(List<SysRole> roles) {
        if (roles.isEmpty()) {
            return Map.of();
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        return relationMapper.countUsersByRoleIds(roleIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("roleId")).longValue(),
                        row -> ((Number) row.get("userCount")).longValue()));
    }

    private void copy(SysRole role, SysRoleVO vo) {
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleCode(role.getRoleCode());
        vo.setSort(role.getSort());
        vo.setStatus(role.getStatus());
        vo.setRemark(role.getRemark());
        vo.setVersion(role.getVersion());
        vo.setCreateTime(role.getCreateTime());
    }

    private SysRoleCache toCache(SysRole role) {
        SysRoleCache cache = new SysRoleCache();
        cache.setId(role.getId());
        cache.setRoleName(role.getRoleName());
        cache.setRoleCode(role.getRoleCode());
        cache.setSort(role.getSort());
        cache.setStatus(role.getStatus());
        cache.setRemark(role.getRemark());
        cache.setVersion(role.getVersion());
        cache.setCreateTime(role.getCreateTime());
        return cache;
    }

    private void copy(SysRoleCache role, SysRoleDetailVO vo) {
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleCode(role.getRoleCode());
        vo.setSort(role.getSort());
        vo.setStatus(role.getStatus());
        vo.setRemark(role.getRemark());
        vo.setVersion(role.getVersion());
        vo.setCreateTime(role.getCreateTime());
    }
}
