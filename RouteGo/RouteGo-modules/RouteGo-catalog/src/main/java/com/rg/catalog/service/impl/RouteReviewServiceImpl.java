package com.rg.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.catalog.dto.RouteReviewQuery;
import com.rg.catalog.dto.RouteReviewReplyRequest;
import com.rg.catalog.entity.Route;
import com.rg.catalog.entity.RouteReview;
import com.rg.catalog.mapper.RouteMapper;
import com.rg.catalog.mapper.RouteReviewMapper;
import com.rg.catalog.service.RouteReviewService;
import com.rg.catalog.vo.RouteReviewVO;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.CatalogException;
import com.rg.common.web.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 线路评价业务实现。
 */
@Service
public class RouteReviewServiceImpl extends ServiceImpl<RouteReviewMapper, RouteReview>
        implements RouteReviewService {

    private final RouteReviewMapper reviewMapper;
    private final RouteMapper routeMapper;

    public RouteReviewServiceImpl(RouteReviewMapper reviewMapper, RouteMapper routeMapper) {
        this.reviewMapper = reviewMapper;
        this.routeMapper = routeMapper;
    }

    @Override
    public PageResult<RouteReviewVO> page(RouteReviewQuery query) {
        LambdaQueryWrapper<RouteReview> wrapper = Wrappers.lambdaQuery();
        if (query.getRouteId() != null) {
            wrapper.eq(RouteReview::getRouteId, query.getRouteId());
        }
        if (query.getRating() != null) {
            wrapper.eq(RouteReview::getRating, query.getRating());
        }
        if (query.getStatus() != null) {
            wrapper.eq(RouteReview::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(RouteReview::getId);
        Page<RouteReview> page = reviewMapper.selectPage(query.toPage(), wrapper);
        List<RouteReviewVO> records = page.getRecords().stream().map(this::toVO).toList();

        Set<Long> routeIds = page.getRecords().stream()
                .map(RouteReview::getRouteId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!routeIds.isEmpty()) {
            Map<Long, String> titleById = routeMapper.selectBatchIds(routeIds).stream()
                    .collect(Collectors.toMap(Route::getId, Route::getTitle));
            records.forEach(vo -> vo.setRouteTitle(titleById.get(vo.getRouteId())));
        }
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public RouteReviewVO detail(Long id) {
        return toVO(requireReview(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long id, RouteReviewReplyRequest request) {
        RouteReview existing = requireReview(id);
        RouteReview update = new RouteReview();
        update.setId(existing.getId());
        update.setVersion(existing.getVersion());
        update.setReply(request.getReply().trim());
        update.setReplyTime(LocalDateTime.now());
        if (reviewMapper.updateById(update) == 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Byte status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new CatalogException(ErrorCode.BAD_REQUEST, "状态只能是 0（隐藏）或 1（显示）");
        }
        RouteReview existing = requireReview(id);
        RouteReview update = new RouteReview();
        update.setId(existing.getId());
        update.setVersion(existing.getVersion());
        update.setStatus(status);
        if (reviewMapper.updateById(update) == 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long id) {
        requireReview(id);
        reviewMapper.deleteById(id);
    }

    private RouteReview requireReview(Long id) {
        RouteReview review = id == null ? null : reviewMapper.selectById(id);
        if (review == null) {
            throw new CatalogException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        return review;
    }

    private RouteReviewVO toVO(RouteReview review) {
        RouteReviewVO vo = new RouteReviewVO();
        vo.setId(review.getId());
        vo.setRouteId(review.getRouteId());
        vo.setOrderId(review.getOrderId());
        vo.setCustomerId(review.getCustomerId());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setImages(review.getImages());
        vo.setReply(review.getReply());
        vo.setReplyTime(review.getReplyTime());
        vo.setStatus(review.getStatus());
        vo.setCreateTime(review.getCreateTime());
        return vo;
    }
}
