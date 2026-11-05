package com.rg.catalog.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.catalog.dto.RouteDepartureQuery;
import com.rg.catalog.dto.RouteDepartureSaveRequest;
import com.rg.catalog.service.RouteDepartureService;
import com.rg.catalog.vo.RouteDepartureVO;
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

import java.util.List;

@Tag(name = "出发班期", description = "线路班期、价格、库存与停售")
@RestController
@RequestMapping("/catalog/route-departure")
public class RouteDepartureController {

    private final RouteDepartureService departureService;

    public RouteDepartureController(RouteDepartureService departureService) {
        this.departureService = departureService;
    }

    @Operation(summary = "班期分页")
    @SaCheckPermission("catalog:RouteDeparture:page")
    @GetMapping("/page")
    public Result<PageResult<RouteDepartureVO>> page(@Valid RouteDepartureQuery query) {
        return Result.success(departureService.page(query));
    }

    @Operation(summary = "线路班期列表", description = "按出发日期升序")
    @SaCheckPermission("catalog:RouteDeparture:list")
    @GetMapping("/list/{routeId}")
    public Result<List<RouteDepartureVO>> list(@Parameter(description = "线路 ID") @PathVariable Long routeId) {
        return Result.success(departureService.listByRoute(routeId));
    }

    @Operation(summary = "新增班期", description = "同一线路同一天只能有一个班期")
    @SaCheckPermission("catalog:RouteDeparture:create")
    @OperLog(title = "班期管理", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody RouteDepartureSaveRequest request) {
        return Result.success(departureService.create(request));
    }

    @Operation(summary = "编辑班期", description = "库存不能小于已售数量")
    @SaCheckPermission("catalog:RouteDeparture:update")
    @OperLog(title = "班期管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "班期 ID") @PathVariable Long id,
                               @Valid @RequestBody RouteDepartureSaveRequest request) {
        departureService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "停售/恢复班期")
    @SaCheckPermission("catalog:RouteDeparture:updateStatus")
    @OperLog(title = "班期管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "班期 ID") @PathVariable Long id,
                                     @Parameter(description = "状态：0 停售，1 可售") @RequestParam Byte status) {
        departureService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除班期", description = "已产生成交的班期不允许删除，请改为停售")
    @SaCheckPermission("catalog:RouteDeparture:delete")
    @OperLog(title = "班期管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "班期 ID") @PathVariable Long id) {
        departureService.deleteDeparture(id);
        return Result.success();
    }
}
