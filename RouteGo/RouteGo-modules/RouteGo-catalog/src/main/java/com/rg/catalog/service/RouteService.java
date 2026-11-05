package com.rg.catalog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.catalog.dto.RouteQuery;
import com.rg.catalog.dto.RouteSaveRequest;
import com.rg.catalog.entity.Route;
import com.rg.catalog.vo.RouteVO;
import com.rg.common.web.PageResult;

/**
 * 线路主表服务。
 */
public interface RouteService extends IService<Route> {

    PageResult<RouteVO> page(RouteQuery query);

    RouteVO detail(Long id);

    Long create(RouteSaveRequest request);

    void update(Long id, RouteSaveRequest request);

    void updateStatus(Long id, Byte status);

    void deleteRoute(Long id);
}
