package com.rg.catalog.service;

import com.rg.catalog.entity.Route;
import com.rg.catalog.entity.RouteImage;
import com.rg.catalog.mapper.RouteCategoryMapper;
import com.rg.catalog.mapper.RouteImageMapper;
import com.rg.catalog.mapper.RouteItineraryMapper;
import com.rg.catalog.mapper.RouteMapper;
import com.rg.catalog.mapper.RouteTagMapper;
import com.rg.catalog.mapper.RouteTagRelMapper;
import com.rg.catalog.service.impl.RouteImageServiceImpl;
import com.rg.catalog.service.impl.RouteServiceImpl;
import com.rg.catalog.support.RouteCacheKeys;
import com.rg.catalog.vo.RouteVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.storage.TempUrlSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 线路详情缓存：命中不打库、未命中回源、图集变更后失效。
 */
@ExtendWith(MockitoExtension.class)
class RouteCacheTest {

    @Mock
    private RouteMapper routeMapper;
    @Mock
    private RouteCategoryMapper categoryMapper;
    @Mock
    private RouteItineraryMapper itineraryMapper;
    @Mock
    private RouteImageMapper imageMapper;
    @Mock
    private RouteTagMapper tagMapper;
    @Mock
    private RouteTagRelMapper tagRelMapper;
    @Mock
    private TempUrlSigner tempUrlSigner;
    @Mock
    private CacheHelper cacheHelper;

    private RouteServiceImpl routeService;

    @BeforeEach
    void setUp() {
        routeService = new RouteServiceImpl(routeMapper, categoryMapper, itineraryMapper,
                imageMapper, tagMapper, tagRelMapper, tempUrlSigner, cacheHelper);
    }

    @Test
    void detailHitReturnsCachedValueWithoutQuerying() {
        RouteVO cached = new RouteVO();
        cached.setId(5L);
        cached.setTitle("缓存里的线路");
        when(cacheHelper.getOrLoad(eq("catalog:route:detail:5"), any(Duration.class), any()))
                .thenReturn(cached);

        RouteVO vo = routeService.detail(5L);

        assertThat(vo.getTitle()).isEqualTo("缓存里的线路");
        verify(routeMapper, never()).selectById(anyLong());
    }

    @Test
    void detailMissLoadsFromDatabaseAndAssemblesRelations() {
        Route route = new Route();
        route.setId(5L);
        route.setTitle("洱海环线");
        route.setStatus((byte) 1);
        when(cacheHelper.getOrLoad(eq("catalog:route:detail:5"), any(Duration.class), any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(2)).get());
        when(routeMapper.selectById(5L)).thenReturn(route);
        when(imageMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());
        when(itineraryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());
        when(tagRelMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        RouteVO vo = routeService.detail(5L);

        assertThat(vo.getTitle()).isEqualTo("洱海环线");
        assertThat(vo.getImages()).isEmpty();
        assertThat(vo.getItineraries()).isEmpty();
        assertThat(vo.getTags()).isEmpty();
    }

    @Test
    void detailUsesThirtyMinuteTtl() {
        when(cacheHelper.getOrLoad(any(), any(Duration.class), any())).thenReturn(new RouteVO());

        routeService.detail(9L);

        verify(cacheHelper).getOrLoad(eq("catalog:route:detail:9"),
                eq(Duration.ofMinutes(30)), any());
    }

    @Test
    void addingImageEvictsRouteDetail() {
        RouteImageMapper mapper = imageMapper;
        RouteImageServiceImpl imageService = new RouteImageServiceImpl(
                mapper, routeMapper, tempUrlSigner, cacheHelper);

        Route route = new Route();
        route.setId(5L);
        when(routeMapper.selectById(5L)).thenReturn(route);
        when(tempUrlSigner.canonicalize("/admin/system/file/3/download"))
                .thenReturn("/admin/system/file/3/download");
        when(mapper.insert(ArgumentMatchers.any(RouteImage.class))).thenReturn(1);

        com.rg.catalog.dto.RouteImageSaveRequest request = new com.rg.catalog.dto.RouteImageSaveRequest();
        request.setRouteId(5L);
        request.setImageUrl("/admin/system/file/3/download");
        imageService.create(request);

        verify(cacheHelper).evictAfterCommit(RouteCacheKeys.detail(5L));
    }
}
