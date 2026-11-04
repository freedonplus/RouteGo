package com.rg.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.admin.dto.MenuCreateRequest;
import com.rg.admin.dto.MenuUpdateRequest;
import com.rg.admin.entity.SysMenu;
import com.rg.admin.mapper.AdminRelationMapper;
import com.rg.admin.mapper.SysMenuMapper;
import com.rg.admin.mapper.SysRoleMapper;
import com.rg.admin.mapper.SysRoleMenuMapper;
import com.rg.admin.service.SysMenuService;
import com.rg.admin.vo.MenuNodeVO;
import com.rg.admin.vo.SysMenuVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单树构建、父子环校验与缓存失效。
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private static final Duration MENU_CACHE_TTL = Duration.ofMinutes(10);
    private static final Set<String> PROTECTED_ROLE_CODES = Set.of("admin", "super_admin");

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleMapper roleMapper;
    private final AdminRelationMapper relationMapper;
    private final CacheHelper cacheHelper;

    public SysMenuServiceImpl(SysMenuMapper menuMapper,
                              SysRoleMenuMapper roleMenuMapper,
                              SysRoleMapper roleMapper,
                              AdminRelationMapper relationMapper,
                              CacheHelper cacheHelper) {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.roleMapper = roleMapper;
        this.relationMapper = relationMapper;
        this.cacheHelper = cacheHelper;
    }

    @Override
    public List<MenuNodeVO> tree() {
        return cacheHelper.getOrLoad(CacheKeyConstants.Admin.MENU_TREE, MENU_CACHE_TTL,
                () -> buildTree(menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                        .orderByAsc(SysMenu::getSort).orderByAsc(SysMenu::getId))));
    }

    @Override
    public List<MenuNodeVO> userTree(Long userId) {
        return cacheHelper.getOrLoad(
                String.format(CacheKeyConstants.Admin.USER_MENU_TREE, userId),
                MENU_CACHE_TTL,
                () -> {
                    List<String> roleCodes = roleMapper.selectRoleCodesByUserId(userId);
                    List<SysMenu> menus;
                    if (roleCodes.contains("admin") || roleCodes.contains("super_admin")) {
                        menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                                .eq(SysMenu::getStatus, 1)
                                .eq(SysMenu::getVisible, 1)
                                .in(SysMenu::getMenuType, List.of("M", "C"))
                                .orderByAsc(SysMenu::getSort).orderByAsc(SysMenu::getId));
                    } else {
                        List<Long> menuIds = relationMapper.selectMenuIdsByUserId(userId);
                        if (menuIds.isEmpty()) {
                            return List.of();
                        }
                        menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                                .in(SysMenu::getId, menuIds)
                                .eq(SysMenu::getStatus, 1)
                                .eq(SysMenu::getVisible, 1)
                                .in(SysMenu::getMenuType, List.of("M", "C"))
                                .orderByAsc(SysMenu::getSort).orderByAsc(SysMenu::getId));
                    }
                    return buildTree(menus);
                });
    }

    @Override
    public SysMenuVO detail(Long id) {
        SysMenu menu = cacheHelper.getOrLoad(
                String.format(CacheKeyConstants.Admin.MENU_BY_ID, id),
                MENU_CACHE_TTL,
                () -> menuMapper.selectById(id));
        if (menu == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        SysMenuVO vo = new SysMenuVO();
        copy(menu, vo);
        return vo;
    }

    @Override
    @Transactional
    public Long createMenu(MenuCreateRequest request) {
        Long parentId = request.getParentId() == null ? 0L : request.getParentId();
        validateParent(null, parentId);
        SysMenu menu = new SysMenu();
        menu.setParentId(parentId);
        menu.setMenuName(request.getMenuName());
        menu.setMenuType(request.getMenuType());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setPerms(request.getPerms());
        menu.setIcon(request.getIcon());
        menu.setSort(request.getSort() == null ? 0 : request.getSort());
        menu.setVisible(request.getVisible() == null ? (byte) 1 : request.getVisible());
        menu.setStatus(request.getStatus() == null ? (byte) 1 : request.getStatus());
        menuMapper.insert(menu);
        cacheHelper.evictAfterCommit(CacheKeyConstants.Admin.MENU_TREE);
        return menu.getId();
    }

    @Override
    @Transactional
    public void updateMenu(Long id, MenuUpdateRequest request) {
        SysMenu existing = menuMapper.selectById(id);
        if (existing == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        Long parentId = request.getParentId() == null ? 0L : request.getParentId();
        validateParent(id, parentId);

        SysMenu update = new SysMenu();
        update.setId(id);
        update.setVersion(request.getVersion());
        update.setParentId(parentId);
        update.setMenuName(request.getMenuName());
        update.setMenuType(request.getMenuType());
        update.setPath(request.getPath());
        update.setComponent(request.getComponent());
        update.setPerms(request.getPerms());
        update.setIcon(request.getIcon());
        update.setSort(request.getSort());
        update.setVisible(request.getVisible());
        if (menuMapper.updateById(update) == 0) {
            throw new AdminException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试");
        }
        cacheHelper.evictAfterCommit(invalidationKeys(id));
    }

    @Override
    @Transactional
    public void deleteMenu(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "菜单不存在");
        }
        if (menuMapper.selectCount(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, id)) > 0) {
            throw new AdminException(ErrorCode.CONFLICT, "存在子菜单，不能删除");
        }
        if (relationMapper.countRolesByMenuId(id) > 0) {
            throw new AdminException(ErrorCode.CONFLICT, "菜单已被角色引用，不能删除");
        }
        String[] keys = invalidationKeys(id);
        menuMapper.deleteById(id);
        cacheHelper.evictAfterCommit(keys);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Byte status) {
        requireMenu(id);
        menuMapper.update(null, Wrappers.<SysMenu>lambdaUpdate()
                .set(SysMenu::getStatus, status)
                .eq(SysMenu::getId, id));
        cacheHelper.evictAfterCommit(invalidationKeys(id));
    }

    @Override
    @Transactional
    public void updateVisible(Long id, Byte visible) {
        requireMenu(id);
        menuMapper.update(null, Wrappers.<SysMenu>lambdaUpdate()
                .set(SysMenu::getVisible, visible)
                .eq(SysMenu::getId, id));
        cacheHelper.evictAfterCommit(invalidationKeys(id));
    }

    private void requireMenu(Long id) {
        if (menuMapper.selectById(id) == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "菜单不存在");
        }
    }

    private void validateParent(Long id, Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (parentId.equals(id)) {
            throw new AdminException("父级不能是自身");
        }
        Long current = parentId;
        int guard = 0;
        while (current != null && current != 0L && guard++ < 100) {
            SysMenu parent = menuMapper.selectById(current);
            if (parent == null) {
                throw new AdminException("父级菜单不存在");
            }
            if (parent.getId().equals(id)) {
                throw new AdminException("父级不能是自身或后代");
            }
            current = parent.getParentId();
        }
    }

    private String[] invalidationKeys(Long menuId) {
        Set<String> keys = new LinkedHashSet<>();
        keys.add(String.format(CacheKeyConstants.Admin.MENU_BY_ID, menuId));
        keys.add(CacheKeyConstants.Admin.MENU_TREE);
        relationMapper.selectRoleIdsByMenuId(menuId).forEach(roleId ->
                keys.add(String.format(CacheKeyConstants.Admin.ROLE_MENUS, roleId)));
        relationMapper.selectUserIdsByMenuId(menuId).forEach(userId ->
                keys.add(String.format(CacheKeyConstants.Admin.USER_MENU_TREE, userId)));
        return keys.toArray(String[]::new);
    }

    private List<MenuNodeVO> buildTree(List<SysMenu> menus) {
        List<SysMenu> sorted = new ArrayList<>(menus);
        sorted.sort(Comparator.comparing(SysMenu::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SysMenu::getId));
        Map<Long, List<SysMenu>> byParent = sorted.stream()
                .collect(Collectors.groupingBy(menu -> menu.getParentId() == null ? 0L : menu.getParentId(),
                        Collectors.toCollection(ArrayList::new)));
        return buildChildren(0L, byParent);
    }

    private List<MenuNodeVO> buildChildren(Long parentId, Map<Long, List<SysMenu>> byParent) {
        List<SysMenu> children = byParent.getOrDefault(parentId, List.of());
        List<MenuNodeVO> nodes = new ArrayList<>(children.size());
        for (SysMenu child : children) {
            MenuNodeVO node = new MenuNodeVO();
            copy(child, node);
            node.setChildren(buildChildren(child.getId(), byParent));
            nodes.add(node);
        }
        return nodes;
    }

    private void copy(SysMenu menu, MenuNodeVO node) {
        node.setId(menu.getId());
        node.setParentId(menu.getParentId());
        node.setMenuName(menu.getMenuName());
        node.setMenuType(menu.getMenuType());
        node.setPath(menu.getPath());
        node.setComponent(menu.getComponent());
        node.setPerms(menu.getPerms());
        node.setIcon(menu.getIcon());
        node.setSort(menu.getSort());
        node.setVisible(menu.getVisible());
        node.setStatus(menu.getStatus());
    }

    private void copy(SysMenu menu, SysMenuVO vo) {
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuType(menu.getMenuType());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setPerms(menu.getPerms());
        vo.setIcon(menu.getIcon());
        vo.setSort(menu.getSort());
        vo.setVisible(menu.getVisible());
        vo.setStatus(menu.getStatus());
        vo.setVersion(menu.getVersion());
        vo.setCreateTime(menu.getCreateTime());
    }
}
