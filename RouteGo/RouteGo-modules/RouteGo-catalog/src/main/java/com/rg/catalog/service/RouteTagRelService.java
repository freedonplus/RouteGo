package com.rg.catalog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.catalog.dto.RouteTagAssignRequest;
import com.rg.catalog.entity.RouteTagRel;
import com.rg.catalog.vo.RouteTagVO;

import java.util.List;

/**
 * 线路标签关联服务。
 */
public interface RouteTagRelService extends IService<RouteTagRel> {

    List<RouteTagVO> listTags(Long routeId);

    void assign(Long routeId, RouteTagAssignRequest request);

    void remove(Long routeId, Long tagId);
}
