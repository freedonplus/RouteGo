package com.rg.catalog.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.catalog.dto.RouteImageSaveRequest;
import com.rg.catalog.service.RouteImageService;
import com.rg.catalog.vo.RouteImageVO;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "线路图片", description = "线路相册维护")
@RestController
@RequestMapping("/catalog/route-image")
public class RouteImageController {

    private final RouteImageService imageService;

    public RouteImageController(RouteImageService imageService) {
        this.imageService = imageService;
    }

    @Operation(summary = "线路图片列表")
    @SaCheckPermission("catalog:RouteImage:list")
    @GetMapping("/list/{routeId}")
    public Result<List<RouteImageVO>> list(@Parameter(description = "线路 ID") @PathVariable Long routeId) {
        return Result.success(imageService.listByRoute(routeId));
    }

    @Operation(summary = "新增线路图片")
    @SaCheckPermission("catalog:RouteImage:create")
    @OperLog(title = "线路图片", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody RouteImageSaveRequest request) {
        return Result.success(imageService.create(request));
    }

    @Operation(summary = "删除线路图片")
    @SaCheckPermission("catalog:RouteImage:delete")
    @OperLog(title = "线路图片", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "图片 ID") @PathVariable Long id) {
        imageService.deleteImage(id);
        return Result.success();
    }
}
