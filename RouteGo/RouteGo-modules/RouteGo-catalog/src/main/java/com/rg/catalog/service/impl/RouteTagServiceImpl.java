package com.rg.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.catalog.dto.RouteTagQuery;
import com.rg.catalog.dto.RouteTagSaveRequest;
import com.rg.catalog.entity.RouteTag;
import com.rg.catalog.entity.RouteTagRel;
import com.rg.catalog.mapper.RouteTagMapper;
import com.rg.catalog.mapper.RouteTagRelMapper;
import com.rg.catalog.service.RouteTagService;
import com.rg.catalog.vo.RouteTagVO;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.CatalogException;
import com.rg.common.web.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 线路标签业务实现。
 */
@Service
public class RouteTagServiceImpl extends ServiceImpl<RouteTagMapper, RouteTag> implements RouteTagService {

    private final RouteTagMapper tagMapper;
    private final RouteTagRelMapper tagRelMapper;

    public RouteTagServiceImpl(RouteTagMapper tagMapper, RouteTagRelMapper tagRelMapper) {
        this.tagMapper = tagMapper;
        this.tagRelMapper = tagRelMapper;
    }

    @Override
    public PageResult<RouteTagVO> page(RouteTagQuery query) {
        LambdaQueryWrapper<RouteTag> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getTagName())) {
            wrapper.like(RouteTag::getTagName, query.getTagName().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(RouteTag::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(RouteTag::getSort).orderByDesc(RouteTag::getId);
        Page<RouteTag> page = tagMapper.selectPage(query.toPage(), wrapper);
        List<RouteTagVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public List<RouteTagVO> listEnabled() {
        return tagMapper.selectList(Wrappers.<RouteTag>lambdaQuery()
                        .eq(RouteTag::getStatus, (byte) 1)
                        .orderByAsc(RouteTag::getSort)
                        .orderByAsc(RouteTag::getId)).stream()
                .map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RouteTagSaveRequest request) {
        String tagName = request.getTagName().trim();
        if (tagMapper.countByTagNameIncludingDeleted(tagName) > 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "标签名称已存在（可能已被删除）");
        }
        RouteTag tag = new RouteTag();
        tag.setTagName(tagName);
        tag.setColor(request.getColor());
        tag.setSort(request.getSort() == null ? 0 : request.getSort());
        tag.setStatus(request.getStatus() == null ? (byte) 1 : request.getStatus());
        tagMapper.insert(tag);
        return tag.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RouteTagSaveRequest request) {
        RouteTag existing = requireTag(id);
        String tagName = request.getTagName().trim();
        if (tagMapper.countByTagNameExcludingId(tagName, id) > 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "标签名称已存在");
        }
        RouteTag update = new RouteTag();
        update.setId(existing.getId());
        update.setTagName(tagName);
        update.setColor(request.getColor());
        update.setSort(request.getSort() == null ? 0 : request.getSort());
        if (request.getStatus() != null) {
            update.setStatus(request.getStatus());
        }
        tagMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Byte status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new CatalogException(ErrorCode.BAD_REQUEST, "状态只能是 0（禁用）或 1（启用）");
        }
        RouteTag existing = requireTag(id);
        RouteTag update = new RouteTag();
        update.setId(existing.getId());
        update.setStatus(status);
        tagMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        requireTag(id);
        // 关联表没有逻辑删除列，直接物理清理，避免留下指向已删标签的孤儿关系
        tagRelMapper.delete(Wrappers.<RouteTagRel>lambdaQuery().eq(RouteTagRel::getTagId, id));
        tagMapper.deleteById(id);
    }

    private RouteTag requireTag(Long id) {
        RouteTag tag = id == null ? null : tagMapper.selectById(id);
        if (tag == null) {
            throw new CatalogException(ErrorCode.NOT_FOUND, "标签不存在");
        }
        return tag;
    }

    private RouteTagVO toVO(RouteTag tag) {
        RouteTagVO vo = new RouteTagVO();
        vo.setId(tag.getId());
        vo.setTagName(tag.getTagName());
        vo.setColor(tag.getColor());
        vo.setSort(tag.getSort());
        vo.setStatus(tag.getStatus());
        vo.setCreateTime(tag.getCreateTime());
        return vo;
    }
}
