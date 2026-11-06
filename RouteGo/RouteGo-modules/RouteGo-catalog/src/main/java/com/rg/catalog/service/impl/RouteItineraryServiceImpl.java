package com.rg.catalog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.catalog.dto.RouteItineraryBatchRequest;
import com.rg.catalog.dto.RouteItinerarySaveRequest;
import com.rg.catalog.entity.Route;
import com.rg.catalog.entity.RouteItinerary;
import com.rg.catalog.mapper.RouteItineraryMapper;
import com.rg.catalog.mapper.RouteMapper;
import com.rg.catalog.service.RouteItineraryService;
import com.rg.catalog.support.RouteCacheKeys;
import com.rg.catalog.vo.RouteItineraryVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.CatalogException;
import com.rg.common.storage.TempUrlSigner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 行程明细业务实现。
 */
@Service
public class RouteItineraryServiceImpl extends ServiceImpl<RouteItineraryMapper, RouteItinerary>
        implements RouteItineraryService {

    private final RouteItineraryMapper itineraryMapper;
    private final RouteMapper routeMapper;
    private final TempUrlSigner tempUrlSigner;
    private final CacheHelper cacheHelper;

    public RouteItineraryServiceImpl(RouteItineraryMapper itineraryMapper, RouteMapper routeMapper,
                                     TempUrlSigner tempUrlSigner, CacheHelper cacheHelper) {
        this.itineraryMapper = itineraryMapper;
        this.routeMapper = routeMapper;
        this.tempUrlSigner = tempUrlSigner;
        this.cacheHelper = cacheHelper;
    }

    @Override
    public List<RouteItineraryVO> listByRoute(Long routeId) {
        return itineraryMapper.selectList(Wrappers.<RouteItinerary>lambdaQuery()
                        .eq(RouteItinerary::getRouteId, routeId)
                        .orderByAsc(RouteItinerary::getDayNo)).stream()
                .map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RouteItinerarySaveRequest request) {
        requireRoute(request.getRouteId());
        checkDayUnique(request.getRouteId(), request.getDayNo(), null);

        RouteItinerary itinerary = new RouteItinerary();
        applyRequest(itinerary, request);
        itinerary.setRouteId(request.getRouteId());
        itineraryMapper.insert(itinerary);
        cacheHelper.evictAfterCommit(RouteCacheKeys.detail(request.getRouteId()));
        return itinerary.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RouteItinerarySaveRequest request) {
        RouteItinerary existing = requireItinerary(id);
        checkDayUnique(existing.getRouteId(), request.getDayNo(), id);

        RouteItinerary update = new RouteItinerary();
        update.setId(existing.getId());
        applyRequest(update, request);
        // 行程不允许跨线路搬迁
        update.setRouteId(existing.getRouteId());
        itineraryMapper.updateById(update);
        cacheHelper.evictAfterCommit(RouteCacheKeys.detail(existing.getRouteId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItinerary(Long id) {
        RouteItinerary existing = requireItinerary(id);
        itineraryMapper.deletePhysicallyById(id);
        cacheHelper.evictAfterCommit(RouteCacheKeys.detail(existing.getRouteId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(Long routeId, RouteItineraryBatchRequest request) {
        requireRoute(routeId);
        List<RouteItinerarySaveRequest> items = request.getItems();

        // 天数不可重复（唯一索引兜底，这里先给出可读的报错）
        long distinctDays = items.stream().map(RouteItinerarySaveRequest::getDayNo).distinct().count();
        if (distinctDays != items.size()) {
            throw new CatalogException(ErrorCode.BAD_REQUEST, "行程天数不能重复");
        }

        itineraryMapper.deletePhysicallyByRouteId(routeId);
        for (RouteItinerarySaveRequest item : items) {
            RouteItinerary itinerary = new RouteItinerary();
            applyRequest(itinerary, item);
            itinerary.setRouteId(routeId);
            itineraryMapper.insert(itinerary);
        }
        cacheHelper.evictAfterCommit(RouteCacheKeys.detail(routeId));
    }

    private Route requireRoute(Long routeId) {
        Route route = routeId == null ? null : routeMapper.selectById(routeId);
        if (route == null) {
            throw new CatalogException(ErrorCode.NOT_FOUND, "线路不存在");
        }
        return route;
    }

    private RouteItinerary requireItinerary(Long id) {
        RouteItinerary itinerary = id == null ? null : itineraryMapper.selectById(id);
        if (itinerary == null) {
            throw new CatalogException(ErrorCode.NOT_FOUND, "行程不存在");
        }
        return itinerary;
    }

    private void checkDayUnique(Long routeId, Integer dayNo, Long excludeId) {
        var wrapper = Wrappers.<RouteItinerary>lambdaQuery()
                .eq(RouteItinerary::getRouteId, routeId)
                .eq(RouteItinerary::getDayNo, dayNo);
        if (excludeId != null) {
            wrapper.ne(RouteItinerary::getId, excludeId);
        }
        if (itineraryMapper.selectCount(wrapper) > 0) {
            throw new CatalogException(ErrorCode.CONFLICT, "第 " + dayNo + " 天的行程已存在");
        }
    }

    private void applyRequest(RouteItinerary itinerary, RouteItinerarySaveRequest request) {
        itinerary.setDayNo(request.getDayNo());
        itinerary.setTitle(request.getTitle());
        itinerary.setDescription(request.getDescription());
        itinerary.setMeals(request.getMeals());
        itinerary.setHotel(request.getHotel());
        itinerary.setTransport(request.getTransport());
        // 还原为标准下载地址：临时签名链接有过期时间，不能落库
        itinerary.setImage(tempUrlSigner.canonicalize(request.getImage()));
        itinerary.setLongitude(request.getLongitude());
        itinerary.setLatitude(request.getLatitude());
        itinerary.setAddress(request.getAddress());
        itinerary.setSort(request.getSort() == null ? 0 : request.getSort());
    }

    private RouteItineraryVO toVO(RouteItinerary itinerary) {
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
}
