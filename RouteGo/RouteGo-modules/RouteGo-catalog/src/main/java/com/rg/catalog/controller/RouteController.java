package com.rg.catalog.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.catalog.dto.RouteQuery;
import com.rg.catalog.dto.RouteSaveRequest;
import com.rg.catalog.service.RouteService;
import com.rg.catalog.vo.RouteVO;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 线路管理。
 * <p>
 * 权限码规范：{@code catalog:<Controller>:<method>}，与 {@code @SaCheckPermission} 一致。
 */
@Tag(name = "线路管理", description = "线路 CRUD、上下架与详情（含行程/图片/标签）")
@RestController
@RequestMapping("/catalog/route")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @Operation(summary = "线路分页")
    @SaCheckPermission("catalog:Route:page")
    @GetMapping("/page")
    public Result<PageResult<RouteVO>> page(@Valid RouteQuery query) {
        return Result.success(routeService.page(query));
    }

    @Operation(summary = "线路详情", description = "同时返回分类名称、标签、图片与行程明细")
    @SaCheckPermission("catalog:Route:detail")
    @GetMapping("/{id}")
    public Result<RouteVO> detail(@Parameter(description = "线路 ID") @PathVariable Long id) {
        return Result.success(routeService.detail(id));
    }

    @Operation(summary = "新增线路")
    @SaCheckPermission("catalog:Route:create")
    @OperLog(title = "线路管理", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody RouteSaveRequest request) {
        return Result.success(routeService.create(request));
    }

    @Operation(summary = "编辑线路", description = "线路编号、销量与评分不可通过本接口修改")
    @SaCheckPermission("catalog:Route:update")
    @OperLog(title = "线路管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "线路 ID") @PathVariable Long id,
                               @Valid @RequestBody RouteSaveRequest request) {
        routeService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "线路上下架")
    @SaCheckPermission("catalog:Route:updateStatus")
    @OperLog(title = "线路管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "线路 ID") @PathVariable Long id,
                                     @Parameter(description = "状态：0 下架，1 上架") @RequestParam Byte status) {
        routeService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除线路", description = "同时逻辑删除行程、图片与标签关联")
    @SaCheckPermission("catalog:Route:delete")
    @OperLog(title = "线路管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "线路 ID") @PathVariable Long id) {
        routeService.deleteRoute(id);
        return Result.success();
    }
}
