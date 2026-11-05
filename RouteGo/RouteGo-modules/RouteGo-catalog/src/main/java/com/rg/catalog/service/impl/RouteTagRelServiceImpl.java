package com.rg.catalog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.catalog.dto.RouteTagAssignRequest;
import com.rg.catalog.entity.Route;
import com.rg.catalog.entity.RouteTag;
import com.rg.catalog.entity.RouteTagRel;
import com.rg.catalog.mapper.RouteMapper;
import com.rg.catalog.mapper.RouteTagMapper;
import com.rg.catalog.mapper.RouteTagRelMapper;
import com.rg.catalog.service.RouteTagRelService;
import com.rg.catalog.vo.RouteTagVO;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.CatalogException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 线路标签关联业务实现。
 */
@Service
public class RouteTagRelServiceImpl extends ServiceImpl<RouteTagRelMapper, RouteTagRel>
        implements RouteTagRelService {

    private final RouteTagRelMapper tagRelMapper;
    private final RouteTagMapper tagMapper;
    private final RouteMapper routeMapper;

    public RouteTagRelServiceImpl(RouteTagRelMapper tagRelMapper,
                                  RouteTagMapper tagMapper,
                                  RouteMapper routeMapper) {
        this.tagRelMapper = tagRelMapper;
        this.tagMapper = tagMapper;
        this.routeMapper = routeMapper;
    }

    @Override
    public List<RouteTagVO> listTags(Long routeId) {
        List<RouteTagRel> rels = tagRelMapper.selectList(Wrappers.<RouteTagRel>lambdaQuery()
                .eq(RouteTagRel::getRouteId, routeId));
        if (rels.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> tagIds = rels.stream().map(RouteTagRel::getTagId).toList();
        return tagMapper.selectBatchIds(tagIds).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long routeId, RouteTagAssignRequest request) {
        Route route = routeId == null ? null : routeMapper.selectById(routeId);
        if (route == null) {
            throw new CatalogException(ErrorCode.NOT_FOUND, "线路不存在");
        }
        List<Long> tagIds = request.getTagIds() == null ? List.of()
                : request.getTagIds().stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (!tagIds.isEmpty()) {
            List<RouteTag> tags = tagMapper.selectBatchIds(tagIds);
            if (tags.size() != tagIds.size()) {
                throw new CatalogException(ErrorCode.BAD_REQUEST, "存在无效的标签，请刷新后重试");
            }
        }

        // 先物理清空再重建，避免唯一索引挡住重新绑定
        tagRelMapper.deletePhysicallyByRouteId(routeId);
        for (Long tagId : tagIds) {
            RouteTagRel rel = new RouteTagRel();
            rel.setRouteId(routeId);
            rel.setTagId(tagId);
            tagRelMapper.insert(rel);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long routeId, Long tagId) {
        if (tagRelMapper.deletePhysically(routeId, tagId) == 0) {
            throw new CatalogException(ErrorCode.NOT_FOUND, "该线路未绑定此标签");
        }
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
