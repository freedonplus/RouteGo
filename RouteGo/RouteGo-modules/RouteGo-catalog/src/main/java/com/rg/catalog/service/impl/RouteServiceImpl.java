package com.rg.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.catalog.dto.RouteQuery;
import com.rg.catalog.dto.RouteSaveRequest;
import com.rg.catalog.entity.Route;
import com.rg.catalog.entity.RouteCategory;
import com.rg.catalog.entity.RouteImage;
import com.rg.catalog.entity.RouteItinerary;
import com.rg.catalog.entity.RouteTag;
import com.rg.catalog.entity.RouteTagRel;
import com.rg.catalog.mapper.RouteCategoryMapper;
import com.rg.catalog.mapper.RouteImageMapper;
import com.rg.catalog.mapper.RouteItineraryMapper;
import com.rg.catalog.mapper.RouteMapper;
import com.rg.catalog.mapper.RouteTagMapper;
import com.rg.catalog.mapper.RouteTagRelMapper;
import com.rg.catalog.service.RouteService;
import com.rg.catalog.vo.RouteImageVO;
import com.rg.catalog.vo.RouteItineraryVO;
import com.rg.catalog.vo.RouteTagVO;
import com.rg.catalog.vo.RouteVO;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.CatalogException;
import com.rg.common.web.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 线路主表业务实现。
 */
@Service
public class RouteServiceImpl extends ServiceImpl<RouteMapper, Route> implements RouteService {

    private static final String ROUTE_NO_PREFIX = "RG";
    private static final DateTimeFormatter ROUTE_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RouteMapper routeMapper;
    private final RouteCategoryMapper categoryMapper;
    private final RouteItineraryMapper itineraryMapper;
    private final RouteImageMapper imageMapper;
    private final RouteTagMapper tagMapper;
    private final RouteTagRelMapper tagRelMapper;

    public RouteServiceImpl(RouteMapper routeMapper,
                            RouteCategoryMapper categoryMapper,
                            RouteItineraryMapper itineraryMapper,
                            RouteImageMapper imageMapper,
                            RouteTagMapper tagMapper,
                            RouteTagRelMapper tagRelMapper) {
        this.routeMapper = routeMapper;
        this.categoryMapper = categoryMapper;
        this.itineraryMapper = itineraryMapper;
        this.imageMapper = imageMapper;
        this.tagMapper = tagMapper;
        this.tagRelMapper = tagRelMapper;
    }

    @Override
    public PageResult<RouteVO> page(RouteQuery query) {
        LambdaQueryWrapper<Route> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(Route::getTitle, query.getTitle().trim());
        }
        if (StringUtils.hasText(query.getRouteNo())) {
            wrapper.like(Route::getRouteNo, query.getRouteNo().trim());
        }
        if (StringUtils.hasText(query.getDestination())) {
            wrapper.like(Route::getDestination, query.getDestination().trim());
        }
        if (StringUtils.hasText(query.getDepartureCity())) {
            wrapper.like(Route::getDepartureCity, query.getDepartureCity().trim());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(Route::getCategoryId, query.getCategoryId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Route::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Route::getId);
        Page<Route> page = routeMapper.selectPage(query.toPage(), wrapper);
        List<RouteVO> records = page.getRecords().stream().map(this::toVO).toList();

        // 一次 IN 查询补齐分类名称，避免列表逐行查分类（N+1）
        Set<Long> categoryIds = page.getRecords().stream()
                .map(Route::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!categoryIds.isEmpty()) {
            Map<Long, String> nameById = categoryMapper.selectBatchIds(categoryIds).stream()
                    .collect(Collectors.toMap(RouteCategory::getId, RouteCategory::getName));
            records.forEach(vo -> vo.setCategoryName(nameById.get(vo.getCategoryId())));
        }
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public RouteVO detail(Long id) {
        Route route = requireRoute(id);
        RouteVO vo = toVO(route);

        RouteCategory category = route.getCategoryId() == null ? null
                : categoryMapper.selectById(route.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }

        vo.setImages(imageMapper.selectList(Wrappers.<RouteImage>lambdaQuery()
                        .eq(RouteImage::getRouteId, id)
                        .orderByAsc(RouteImage::getSort)
                        .orderByAsc(RouteImage::getId)).stream()
                .map(this::toImageVO).toList());

        vo.setItineraries(itineraryMapper.selectList(Wrappers.<RouteItinerary>lambdaQuery()
                        .eq(RouteItinerary::getRouteId, id)
                        .orderByAsc(RouteItinerary::getDayNo)).stream()
                .map(this::toItineraryVO).toList());

        List<RouteTagRel> rels = tagRelMapper.selectList(Wrappers.<RouteTagRel>lambdaQuery()
                .eq(RouteTagRel::getRouteId, id));
        if (rels.isEmpty()) {
            vo.setTags(new ArrayList<>());
        } else {
            List<Long> tagIds = rels.stream().map(RouteTagRel::getTagId).toList();
            vo.setTags(tagMapper.selectBatchIds(tagIds).stream().map(this::toTagVO).toList());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RouteSaveRequest request) {
        requireCategory(request.getCategoryId());
        checkDays(request.getDays(), request.getNights());

        String routeNo = StringUtils.hasText(request.getRouteNo())
                ? request.getRouteNo().trim() : generateRouteNo();
        if (routeMapper.countByRouteNoIncludingDeleted(routeNo) > 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "线路编号已存在（可能已被删除）");
        }

        Route route = new Route();
        applyRequest(route, request);
        route.setRouteNo(routeNo);
        route.setSales(0);
        route.setRating(new java.math.BigDecimal("5.00"));
        route.setStatus(request.getStatus() == null ? (byte) 0 : request.getStatus());
        route.setSort(request.getSort() == null ? 0 : request.getSort());
        route.setVersion(0);
        routeMapper.insert(route);
        return route.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RouteSaveRequest request) {
        Route existing = requireRoute(id);
        requireCategory(request.getCategoryId());
        checkDays(request.getDays(), request.getNights());

        Route update = new Route();
        update.setId(existing.getId());
        update.setVersion(existing.getVersion());
        applyRequest(update, request);
        if (request.getStatus() != null) {
            update.setStatus(request.getStatus());
        }
        if (request.getSort() != null) {
            update.setSort(request.getSort());
        }
        // 线路编号与销量/评分不允许通过编辑接口修改
        if (routeMapper.updateById(update) == 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Byte status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new CatalogException(ErrorCode.BAD_REQUEST, "状态只能是 0（下架）或 1（上架）");
        }
        Route existing = requireRoute(id);
        Route update = new Route();
        update.setId(existing.getId());
        update.setVersion(existing.getVersion());
        update.setStatus(status);
        if (routeMapper.updateById(update) == 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoute(Long id) {
        requireRoute(id);
        // 主表与从表一起逻辑删除，避免行程/图片/标签残留在详情里
        routeMapper.deleteById(id);
        itineraryMapper.delete(Wrappers.<RouteItinerary>lambdaQuery().eq(RouteItinerary::getRouteId, id));
        imageMapper.delete(Wrappers.<RouteImage>lambdaQuery().eq(RouteImage::getRouteId, id));
        tagRelMapper.delete(Wrappers.<RouteTagRel>lambdaQuery().eq(RouteTagRel::getRouteId, id));
    }

    private Route requireRoute(Long id) {
        Route route = id == null ? null : routeMapper.selectById(id);
        if (route == null) {
            throw new CatalogException(ErrorCode.NOT_FOUND, "线路不存在");
        }
        return route;
    }

    private void requireCategory(Long categoryId) {
        if (categoryId == null || categoryMapper.selectById(categoryId) == null) {
            throw new CatalogException(ErrorCode.BAD_REQUEST, "线路分类不存在");
        }
    }

    private void checkDays(Integer days, Integer nights) {
        if (days == null || days < 1) {
            throw new CatalogException(ErrorCode.BAD_REQUEST, "行程天数至少 1 天");
        }
        if (nights != null && nights > days) {
            throw new CatalogException(ErrorCode.BAD_REQUEST, "住宿晚数不能大于行程天数");
        }
    }

    private String generateRouteNo() {
        String prefix = ROUTE_NO_PREFIX + LocalDate.now().format(ROUTE_NO_DATE);
        for (int i = 0; i < 10; i++) {
            String candidate = prefix + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            if (routeMapper.countByRouteNoIncludingDeleted(candidate) == 0) {
                return candidate;
            }
        }
        throw new CatalogException(ErrorCode.CONFLICT, "线路编号生成失败，请手动填写线路编号");
    }

    private void applyRequest(Route route, RouteSaveRequest request) {
        route.setCategoryId(request.getCategoryId());
        route.setTitle(request.getTitle());
        route.setSubtitle(request.getSubtitle());
        route.setCoverImage(request.getCoverImage());
        route.setDestination(request.getDestination());
        route.setDepartureCity(request.getDepartureCity());
        route.setLongitude(request.getLongitude());
        route.setLatitude(request.getLatitude());
        route.setAddress(request.getAddress());
        route.setDays(request.getDays());
        route.setNights(request.getNights() == null ? 0 : request.getNights());
        route.setMinPrice(request.getMinPrice());
        route.setOriginalPrice(request.getOriginalPrice());
        route.setHighlights(request.getHighlights());
        route.setIncludeItems(request.getIncludeItems());
        route.setExcludeItems(request.getExcludeItems());
        route.setBookingNotice(request.getBookingNotice());
    }

    private RouteVO toVO(Route route) {
        RouteVO vo = new RouteVO();
        vo.setId(route.getId());
        vo.setCategoryId(route.getCategoryId());
        vo.setRouteNo(route.getRouteNo());
        vo.setTitle(route.getTitle());
        vo.setSubtitle(route.getSubtitle());
        vo.setCoverImage(route.getCoverImage());
        vo.setDestination(route.getDestination());
        vo.setDepartureCity(route.getDepartureCity());
        vo.setLongitude(route.getLongitude());
        vo.setLatitude(route.getLatitude());
        vo.setAddress(route.getAddress());
        vo.setDays(route.getDays());
        vo.setNights(route.getNights());
        vo.setMinPrice(route.getMinPrice());
        vo.setOriginalPrice(route.getOriginalPrice());
        vo.setHighlights(route.getHighlights());
        vo.setIncludeItems(route.getIncludeItems());
        vo.setExcludeItems(route.getExcludeItems());
        vo.setBookingNotice(route.getBookingNotice());
        vo.setSales(route.getSales());
        vo.setRating(route.getRating());
        vo.setStatus(route.getStatus());
        vo.setSort(route.getSort());
        vo.setVersion(route.getVersion());
        vo.setCreateTime(route.getCreateTime());
        vo.setUpdateTime(route.getUpdateTime());
        return vo;
    }

    private RouteImageVO toImageVO(RouteImage image) {
        RouteImageVO vo = new RouteImageVO();
        vo.setId(image.getId());
        vo.setRouteId(image.getRouteId());
        vo.setImageUrl(image.getImageUrl());
        vo.setSort(image.getSort());
        vo.setCreateTime(image.getCreateTime());
        return vo;
    }

    private RouteItineraryVO toItineraryVO(RouteItinerary itinerary) {
        RouteItineraryVO vo = new RouteItineraryVO();
        vo.setId(itinerary.getId());
        vo.setRouteId(itinerary.getRouteId());
        vo.setDayNo(itinerary.getDayNo());
        vo.setTitle(itinerary.getTitle());
        vo.setDescription(itinerary.getDescription());
        vo.setMeals(itinerary.getMeals());
        vo.setHotel(itinerary.getHotel());
        vo.setTransport(itinerary.getTransport());
        vo.setImage(itinerary.getImage());
        vo.setLongitude(itinerary.getLongitude());
        vo.setLatitude(itinerary.getLatitude());
        vo.setAddress(itinerary.getAddress());
        vo.setSort(itinerary.getSort());
        vo.setCreateTime(itinerary.getCreateTime());
        return vo;
    }

    private RouteTagVO toTagVO(RouteTag tag) {
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
