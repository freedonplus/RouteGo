package com.rg.catalog.controller.app;

import com.rg.catalog.dto.RouteQuery;
import com.rg.catalog.service.RouteCategoryService;
import com.rg.catalog.service.RouteDepartureService;
import com.rg.catalog.service.RouteService;
import com.rg.catalog.vo.RouteCategoryVO;
import com.rg.catalog.vo.RouteDepartureVO;
import com.rg.catalog.vo.RouteVO;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端线路浏览（只读）。
 * <p>
 * 后台的 {@code /catalog/**} 带 {@code @SaCheckPermission}，App 用户没有后台角色，
 * 不能直接复用；这里以 {@code /app/**} 暴露只读视图，逻辑仍走同一套 service。
 * 写操作不在此暴露，后台接口保持原有的权限校验。
 */
@Tag(name = "App-线路浏览", description = "C 端只读：分类 / 线路列表 / 详情 / 班期")
@RestController
@RequestMapping("/app")
public class AppRouteController {

    private final RouteService routeService;
    private final RouteDepartureService routeDepartureService;
    private final RouteCategoryService routeCategoryService;

    public AppRouteController(RouteService routeService,
                              RouteDepartureService routeDepartureService,
                              RouteCategoryService routeCategoryService) {
        this.routeService = routeService;
        this.routeDepartureService = routeDepartureService;
        this.routeCategoryService = routeCategoryService;
    }

    @Operation(summary = "线路分页", description = "只返回已上架线路；支持目的地、出发地、分类、关键字筛选。")
    @GetMapping("/routes")
    public Result<PageResult<RouteVO>> routes(@Valid RouteQuery query) {
        // C 端固定只看上架内容，忽略客户端传入的状态，避免读到下架线路
        query.setStatus((byte) 1);
        return Result.success(routeService.page(query));
    }

    @Operation(summary = "线路详情", description = "含分类名称、标签、图片与每日行程。")
    @GetMapping("/routes/{id}")
    public Result<RouteVO> routeDetail(@Parameter(description = "线路 ID") @PathVariable Long id) {
        return Result.success(routeService.detail(id));
    }

    @Operation(summary = "可售班期", description = "按出发日期升序返回该线路的班期与余位。")
    @GetMapping("/routes/{id}/departures")
    public Result<List<RouteDepartureVO>> departures(@Parameter(description = "线路 ID") @PathVariable Long id) {
        return Result.success(routeDepartureService.listByRoute(id));
    }

    @Operation(summary = "线路分类", description = "树形分类，用于首页与列表页筛选。")
    @GetMapping("/categories")
    public Result<List<RouteCategoryVO>> categories() {
        return Result.success(routeCategoryService.tree());
    }
}
