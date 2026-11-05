package com.rg.catalog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.catalog.dto.RouteCategorySaveRequest;
import com.rg.catalog.entity.Route;
import com.rg.catalog.entity.RouteCategory;
import com.rg.catalog.mapper.RouteCategoryMapper;
import com.rg.catalog.mapper.RouteMapper;
import com.rg.catalog.service.RouteCategoryService;
import com.rg.catalog.vo.RouteCategoryVO;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.CatalogException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 线路分类业务实现。
 */
@Service
public class RouteCategoryServiceImpl extends ServiceImpl<RouteCategoryMapper, RouteCategory>
        implements RouteCategoryService {

    private static final long ROOT_ID = 0L;

    private final RouteCategoryMapper categoryMapper;
    private final RouteMapper routeMapper;

    public RouteCategoryServiceImpl(RouteCategoryMapper categoryMapper, RouteMapper routeMapper) {
        this.categoryMapper = categoryMapper;
        this.routeMapper = routeMapper;
    }

    @Override
    public List<RouteCategoryVO> tree() {
        List<RouteCategory> all = categoryMapper.selectList(Wrappers.<RouteCategory>lambdaQuery()
                .orderByAsc(RouteCategory::getSort)
                .orderByAsc(RouteCategory::getId));
        Map<Long, RouteCategoryVO> nodes = new LinkedHashMap<>();
        all.forEach(category -> nodes.put(category.getId(), toVO(category)));

        List<RouteCategoryVO> roots = new ArrayList<>();
        for (RouteCategory category : all) {
            RouteCategoryVO node = nodes.get(category.getId());
            RouteCategoryVO parent = category.getParentId() == null ? null : nodes.get(category.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    @Override
    public RouteCategoryVO detail(Long id) {
        return toVO(requireCategory(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RouteCategorySaveRequest request) {
        long parentId = request.getParentId() == null ? ROOT_ID : request.getParentId();
        if (parentId != ROOT_ID) {
            requireCategory(parentId);
        }
        checkNameUnique(parentId, request.getName(), null);

        RouteCategory category = new RouteCategory();
        category.setParentId(parentId);
        category.setName(request.getName().trim());
        category.setSort(request.getSort() == null ? 0 : request.getSort());
        category.setStatus(request.getStatus() == null ? (byte) 1 : request.getStatus());
        category.setIcon(request.getIcon());
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RouteCategorySaveRequest request) {
        RouteCategory existing = requireCategory(id);
        long parentId = request.getParentId() == null ? ROOT_ID : request.getParentId();
        if (parentId == id) {
            throw new CatalogException(ErrorCode.BAD_REQUEST, "父级分类不能是自己");
        }
        if (parentId != ROOT_ID) {
            requireCategory(parentId);
            assertNotDescendant(id, parentId);
        }
        checkNameUnique(parentId, request.getName(), id);

        RouteCategory update = new RouteCategory();
        update.setId(existing.getId());
        update.setParentId(parentId);
        update.setName(request.getName().trim());
        update.setSort(request.getSort() == null ? 0 : request.getSort());
        if (request.getStatus() != null) {
            update.setStatus(request.getStatus());
        }
        update.setIcon(request.getIcon());
        categoryMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Byte status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new CatalogException(ErrorCode.BAD_REQUEST, "状态只能是 0（禁用）或 1（启用）");
        }
        RouteCategory existing = requireCategory(id);
        RouteCategory update = new RouteCategory();
        update.setId(existing.getId());
        update.setStatus(status);
        categoryMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        requireCategory(id);
        long children = categoryMapper.selectCount(Wrappers.<RouteCategory>lambdaQuery()
                .eq(RouteCategory::getParentId, id));
        if (children > 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "该分类下还有子分类，请先删除子分类");
        }
        long routes = routeMapper.selectCount(Wrappers.<Route>lambdaQuery().eq(Route::getCategoryId, id));
        if (routes > 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "该分类下还有线路，请先调整线路分类");
        }
        categoryMapper.deleteById(id);
    }

    private RouteCategory requireCategory(Long id) {
        RouteCategory category = id == null ? null : categoryMapper.selectById(id);
        if (category == null) {
            throw new CatalogException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private void checkNameUnique(long parentId, String name, Long excludeId) {
        var wrapper = Wrappers.<RouteCategory>lambdaQuery()
                .eq(RouteCategory::getParentId, parentId)
                .eq(RouteCategory::getName, name.trim());
        if (excludeId != null) {
            wrapper.ne(RouteCategory::getId, excludeId);
        }
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "同一父级下已存在同名分类");
        }
    }

    /**
     * 校验 targetParentId 不是当前分类的后代，避免把分类挂到自己的子树下形成环。
     */
    private void assertNotDescendant(Long id, Long targetParentId) {
        Long cursor = targetParentId;
        int guard = 0;
        while (cursor != null && cursor != ROOT_ID && guard++ < 64) {
            if (cursor.equals(id)) {
                throw new CatalogException(ErrorCode.BAD_REQUEST, "不能把分类移动到自己的子分类下");
            }
            RouteCategory parent = categoryMapper.selectById(cursor);
            cursor = parent == null ? null : parent.getParentId();
        }
    }

    private RouteCategoryVO toVO(RouteCategory category) {
        RouteCategoryVO vo = new RouteCategoryVO();
        vo.setId(category.getId());
        vo.setParentId(category.getParentId());
        vo.setName(category.getName());
        vo.setSort(category.getSort());
        vo.setStatus(category.getStatus());
        vo.setIcon(category.getIcon());
        vo.setCreateTime(category.getCreateTime());
        return vo;
    }
}
