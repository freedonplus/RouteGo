package com.rg.catalog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.catalog.dto.RouteItineraryBatchRequest;
import com.rg.catalog.dto.RouteItinerarySaveRequest;
import com.rg.catalog.entity.RouteItinerary;
import com.rg.catalog.vo.RouteItineraryVO;

import java.util.List;

/**
 * 行程明细服务。
 */
public interface RouteItineraryService extends IService<RouteItinerary> {

    List<RouteItineraryVO> listByRoute(Long routeId);

    Long create(RouteItinerarySaveRequest request);

    void update(Long id, RouteItinerarySaveRequest request);

    void deleteItinerary(Long id);

    void saveBatch(Long routeId, RouteItineraryBatchRequest request);
}
