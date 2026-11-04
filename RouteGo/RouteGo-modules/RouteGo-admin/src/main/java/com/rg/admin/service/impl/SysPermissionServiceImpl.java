package com.rg.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.admin.dto.PermissionCreateRequest;
import com.rg.admin.dto.PermissionQuery;
import com.rg.admin.dto.PermissionUpdateRequest;
import com.rg.admin.entity.SysPermission;
import com.rg.admin.mapper.AdminRelationMapper;
import com.rg.admin.mapper.SysPermissionMapper;
import com.rg.admin.mapper.SysRoleMapper;
import com.rg.admin.mapper.SysRolePermissionMapper;
import com.rg.admin.service.SysPermissionService;
import com.rg.admin.vo.PermissionGroupVO;
import com.rg.admin.vo.SysPermissionVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.web.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限点业务实现。
 */
@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>
        implements SysPermissionService {

    private static final Duration PERMISSION_CACHE_TTL = Duration.ofMinutes(10);

    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysRoleMapper roleMapper;
    private final AdminRelationMapper relationMapper;
    private final CacheHelper cacheHelper;

    public SysPermissionServiceImpl(SysPermissionMapper permissionMapper,
                                    SysRolePermissionMapper rolePermissionMapper,
                                    SysRoleMapper roleMapper,
                                    AdminRelationMapper relationMapper,
                                    CacheHelper cacheHelper) {
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.roleMapper = roleMapper;
        this.relationMapper = relationMapper;
        this.cacheHelper = cacheHelper;
    }

    @Override
    public PageResult<SysPermissionVO> page(PermissionQuery query) {
        LambdaQueryWrapper<SysPermission> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getPermissionName())) {
            wrapper.like(SysPermission::getPermissionName, query.getPermissionName());
        }
        if (StringUtils.hasText(query.getPermissionCode())) {
            wrapper.like(SysPermission::getPermissionCode, query.getPermissionCode());
        }
        if (StringUtils.hasText(query.getModule())) {
            wrapper.eq(SysPermission::getModule, query.getModule());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysPermission::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysPermission::getModule).orderByAsc(SysPermission::getId);

        Page<SysPermission> page = permissionMapper.selectPage(query.toPage(), wrapper);
        List<SysPermissionVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public List<PermissionGroupVO> grouped() {
        List<SysPermission> permissions = cacheHelper.getOrLoad(
                CacheKeyConstants.Admin.PERMISSION_ALL,
                PERMISSION_CACHE_TTL,
                () -> permissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery()
                        .orderByAsc(SysPermission::getModule).orderByAsc(SysPermission::getId)));
        Map<String, List<SysPermissionVO>> grouped = new LinkedHashMap<>();
        for (SysPermission permission : permissions) {
            String module = StringUtils.hasText(permission.getModule()) ? permission.getModule() : "未分组";
            grouped.computeIfAbsent(module, key -> new ArrayList<>()).add(toVO(permission));
        }
        return grouped.entrySet().stream()
                .map(entry -> new PermissionGroupVO(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public SysPermissionVO detail(Long id) {
        SysPermission permission = cacheHelper.getOrLoad(
                String.format(CacheKeyConstants.Admin.PERMISSION_BY_ID, id),
                PERMISSION_CACHE_TTL,
                () -> permissionMapper.selectById(id));
        if (permission == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "权限点不存在");
        }
        return toVO(permission);
    }

    @Override
    @Transactional
    public Long createPermission(PermissionCreateRequest request) {
        if (permissionMapper.countByPermissionCodeIncludingDeleted(request.getPermissionCode()) > 0) {
            throw new AdminException(ErrorCode.CONFLICT, "权限码已被使用（可能已删除）");
        }
        SysPermission permission = new SysPermission();
        permission.setPermissionName(request.getPermissionName());
        permission.setPermissionCode(request.getPermissionCode());
        permission.setModule(request.getModule());
        permission.setDescription(request.getDescription());
        permission.setStatus(request.getStatus() == null ? (byte) 1 : request.getStatus());
        permissionMapper.insert(permission);
        cacheHelper.evictAfterCommit(CacheKeyConstants.Admin.PERMISSION_ALL);
        return permission.getId();
    }

    @Override
    @Transactional
    public void updatePermission(Long id, PermissionUpdateRequest request) {
        requirePermission(id);
        SysPermission update = new SysPermission();
        update.setId(id);
        update.setPermissionName(request.getPermissionName());
        update.setModule(request.getModule());
        update.setDescription(request.getDescription());
        if (permissionMapper.updateById(update) == 0) {
            throw new AdminException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试");
        }
        cacheHelper.evictAfterCommit(invalidationKeys(id));
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        requirePermission(id);
        if (relationMapper.countRolesByPermissionId(id) > 0) {
            throw new AdminException(ErrorCode.CONFLICT, "权限点已被角色引用，不能删除");
        }
        String[] keys = invalidationKeys(id);
        permissionMapper.deleteById(id);
        cacheHelper.evictAfterCommit(keys);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Byte status) {
        requirePermission(id);
        permissionMapper.update(null, Wrappers.<SysPermission>lambdaUpdate()
                .set(SysPermission::getStatus, status)
                .eq(SysPermission::getId, id));
        cacheHelper.evictAfterCommit(invalidationKeys(id));
    }

    private void requirePermission(Long id) {
        if (permissionMapper.selectById(id) == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "权限点不存在");
        }
    }

    private String[] invalidationKeys(Long permissionId) {
        Set<String> keys = new LinkedHashSet<>();
        keys.add(String.format(CacheKeyConstants.Admin.PERMISSION_BY_ID, permissionId));
        keys.add(CacheKeyConstants.Admin.PERMISSION_ALL);
        relationMapper.selectRoleIdsByPermissionId(permissionId).forEach(roleId ->
                relationMapper.selectUserIdsByRoleId(roleId).forEach(userId ->
                        keys.add(String.format(CacheKeyConstants.Admin.USER_PERMISSIONS, userId))));
        return keys.toArray(String[]::new);
    }

    private SysPermissionVO toVO(SysPermission permission) {
        SysPermissionVO vo = new SysPermissionVO();
        vo.setId(permission.getId());
        vo.setPermissionName(permission.getPermissionName());
        vo.setPermissionCode(permission.getPermissionCode());
        vo.setModule(permission.getModule());
        vo.setDescription(permission.getDescription());
        vo.setStatus(permission.getStatus());
        vo.setCreateTime(permission.getCreateTime());
        return vo;
    }
}
