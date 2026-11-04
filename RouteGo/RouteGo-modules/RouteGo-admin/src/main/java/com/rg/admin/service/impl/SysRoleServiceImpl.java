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
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        List<SysRoleVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public SysRoleDetailVO detail(Long id) {
        SysRole role = cacheHelper.getOrLoad(
                String.format(CacheKeyConstants.Admin.ROLE_BY_ID, id),
                ROLE_CACHE_TTL,
                () -> roleMapper.selectById(id));
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
    @Transactional
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
    @Transactional
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
        update.setStatus(request.getStatus());
        update.setRemark(request.getRemark());
        if (roleMapper.updateById(update) == 0) {
            throw new AdminException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试");
        }
        cacheHelper.evictAfterCommit(String.format(CacheKeyConstants.Admin.ROLE_BY_ID, id));
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Byte status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new AdminException("状态只能是 0 或 1");
        }
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if (status == 0 && PROTECTED_ROLE_CODES.contains(role.getRoleCode())) {
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
        if (status == 0) {
            userIds.forEach(StpUtil::logout);
        }
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if (PROTECTED_ROLE_CODES.contains(role.getRoleCode())) {
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
        userIds.forEach(StpUtil::logout);
    }

    @Override
    @Transactional
    public void assignMenus(Long id, AssignIdsRequest request) {
        requireRole(id);
        List<Long> menuIds = request.getIds().stream().distinct().toList();
        if (!menuIds.isEmpty() && menuMapper.selectBatchIds(menuIds).size() != menuIds.size()) {
            throw new AdminException("菜单不存在");
        }
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
        for (Long menuId : menuIds) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(id);
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        }

        List<String> keys = new ArrayList<>();
        keys.add(String.format(CacheKeyConstants.Admin.ROLE_MENUS, id));
        relationMapper.selectUserIdsByRoleId(id)
                .forEach(userId -> keys.add(String.format(CacheKeyConstants.Admin.USER_MENU_TREE, userId)));
        cacheHelper.evictAfterCommit(keys.toArray(String[]::new));
    }

    @Override
    @Transactional
    public void assignPermissions(Long id, AssignIdsRequest request) {
        requireRole(id);
        List<Long> permissionIds = request.getIds().stream().distinct().toList();
        if (!permissionIds.isEmpty()) {
            List<SysPermission> permissions = permissionMapper.selectBatchIds(permissionIds);
            boolean invalid = permissions.size() != permissionIds.size()
                    || permissions.stream().anyMatch(p -> p.getStatus() == null || p.getStatus() == 0);
            if (invalid) {
                throw new AdminException("权限点不存在或已禁用");
            }
        }
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, id));
        for (Long permissionId : permissionIds) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleId(id);
            relation.setPermissionId(permissionId);
            rolePermissionMapper.insert(relation);
        }

        List<String> keys = new ArrayList<>();
        keys.add(String.format(CacheKeyConstants.Admin.ROLE_PERMISSIONS, id));
        relationMapper.selectUserIdsByRoleId(id)
                .forEach(userId -> keys.add(String.format(CacheKeyConstants.Admin.USER_PERMISSIONS, userId)));
        cacheHelper.evictAfterCommit(keys.toArray(String[]::new));
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

    private void requireRole(Long id) {
        if (roleMapper.selectById(id) == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "角色不存在");
        }
    }

    private void addUserKeys(List<String> keys, Long userId) {
        keys.add(String.format(CacheKeyConstants.Admin.USER_ROLES, userId));
        keys.add(String.format(CacheKeyConstants.Admin.USER_PERMISSIONS, userId));
        keys.add(String.format(CacheKeyConstants.Admin.USER_MENU_TREE, userId));
    }

    private SysRoleVO toVO(SysRole role) {
        SysRoleVO vo = new SysRoleVO();
        copy(role, vo);
        return vo;
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
}
