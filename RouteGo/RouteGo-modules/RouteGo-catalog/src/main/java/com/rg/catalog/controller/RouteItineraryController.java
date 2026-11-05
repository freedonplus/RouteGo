package com.rg.catalog.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.catalog.dto.RouteItineraryBatchRequest;
import com.rg.catalog.dto.RouteItinerarySaveRequest;
import com.rg.catalog.service.RouteItineraryService;
import com.rg.catalog.vo.RouteItineraryVO;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "行程明细", description = "线路行程按天维护，支持整体覆盖保存")
@RestController
@RequestMapping("/catalog/route-itinerary")
public class RouteItineraryController {

    private final RouteItineraryService itineraryService;

    public RouteItineraryController(RouteItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @Operation(summary = "线路行程列表", description = "按天数升序返回")
    @SaCheckPermission("catalog:RouteItinerary:list")
    @GetMapping("/list/{routeId}")
    public Result<List<RouteItineraryVO>> list(@Parameter(description = "线路 ID") @PathVariable Long routeId) {
        return Result.success(itineraryService.listByRoute(routeId));
    }

    @Operation(summary = "新增单天行程")
    @SaCheckPermission("catalog:RouteItinerary:create")
    @OperLog(title = "行程管理", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody RouteItinerarySaveRequest request) {
        return Result.success(itineraryService.create(request));
    }

    @Operation(summary = "编辑单天行程")
    @SaCheckPermission("catalog:RouteItinerary:update")
    @OperLog(title = "行程管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "行程 ID") @PathVariable Long id,
                               @Valid @RequestBody RouteItinerarySaveRequest request) {
        itineraryService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除单天行程")
    @SaCheckPermission("catalog:RouteItinerary:delete")
    @OperLog(title = "行程管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "行程 ID") @PathVariable Long id) {
        itineraryService.deleteItinerary(id);
        return Result.success();
    }

    @Operation(summary = "整体覆盖保存行程", description = "先清空该线路原有行程，再按提交顺序重建")
    @SaCheckPermission("catalog:RouteItinerary:saveBatch")
    @OperLog(title = "行程管理", type = BusinessType.UPDATE)
    @PutMapping("/batch/{routeId}")
    public Result<Void> saveBatch(@Parameter(description = "线路 ID") @PathVariable Long routeId,
                                  @Valid @RequestBody RouteItineraryBatchRequest request) {
        itineraryService.saveBatch(routeId, request);
        return Result.success();
    }
}
