package com.rg.catalog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.catalog.dto.RouteImageSaveRequest;
import com.rg.catalog.entity.RouteImage;
import com.rg.catalog.vo.RouteImageVO;

import java.util.List;

/**
 * 线路图片服务。
 */
public interface RouteImageService extends IService<RouteImage> {

    List<RouteImageVO> listByRoute(Long routeId);

    Long create(RouteImageSaveRequest request);

    void deleteImage(Long id);
}
