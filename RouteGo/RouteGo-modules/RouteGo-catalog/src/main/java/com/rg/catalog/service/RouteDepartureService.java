package com.rg.catalog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.catalog.dto.RouteDepartureQuery;
import com.rg.catalog.dto.RouteDepartureSaveRequest;
import com.rg.catalog.entity.RouteDeparture;
import com.rg.catalog.vo.RouteDepartureVO;
import com.rg.common.web.PageResult;

import java.util.List;

/**
 * 出发班期服务。
 */
public interface RouteDepartureService extends IService<RouteDeparture> {

    PageResult<RouteDepartureVO> page(RouteDepartureQuery query);

    List<RouteDepartureVO> listByRoute(Long routeId);

    Long create(RouteDepartureSaveRequest request);

    void update(Long id, RouteDepartureSaveRequest request);

    void updateStatus(Long id, Byte status);

    void deleteDeparture(Long id);
}
