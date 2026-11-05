package com.rg.catalog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.catalog.dto.RouteCategorySaveRequest;
import com.rg.catalog.entity.RouteCategory;
import com.rg.catalog.vo.RouteCategoryVO;

import java.util.List;

/**
 * 线路分类服务。
 */
public interface RouteCategoryService extends IService<RouteCategory> {

    List<RouteCategoryVO> tree();

    RouteCategoryVO detail(Long id);

    Long create(RouteCategorySaveRequest request);

    void update(Long id, RouteCategorySaveRequest request);

    void updateStatus(Long id, Byte status);

    void deleteCategory(Long id);
}
