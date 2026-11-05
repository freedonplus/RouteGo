package com.rg.catalog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.catalog.dto.RouteTagQuery;
import com.rg.catalog.dto.RouteTagSaveRequest;
import com.rg.catalog.entity.RouteTag;
import com.rg.catalog.vo.RouteTagVO;
import com.rg.common.web.PageResult;

import java.util.List;

/**
 * 线路标签服务。
 */
public interface RouteTagService extends IService<RouteTag> {

    PageResult<RouteTagVO> page(RouteTagQuery query);

    List<RouteTagVO> listEnabled();

    Long create(RouteTagSaveRequest request);

    void update(Long id, RouteTagSaveRequest request);

    void updateStatus(Long id, Byte status);

    void deleteTag(Long id);
}
