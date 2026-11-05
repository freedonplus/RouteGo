package com.rg.catalog.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.catalog.dto.RouteTagAssignRequest;
import com.rg.catalog.service.RouteTagRelService;
import com.rg.catalog.vo.RouteTagVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "线路标签绑定", description = "线路与标签的多对多关系")
@RestController
@RequestMapping("/catalog/route-tag-rel")
public class RouteTagRelController {

    private final RouteTagRelService tagRelService;

    public RouteTagRelController(RouteTagRelService tagRelService) {
        this.tagRelService = tagRelService;
    }

    @Operation(summary = "线路已绑定标签")
    @SaCheckPermission("catalog:RouteTagRel:list")
    @GetMapping("/list/{routeId}")
    public Result<List<RouteTagVO>> list(@Parameter(description = "线路 ID") @PathVariable Long routeId) {
        return Result.success(tagRelService.listTags(routeId));
    }

    @Operation(summary = "覆盖式绑定标签", description = "传空数组表示清空该线路的全部标签")
    @SaCheckPermission("catalog:RouteTagRel:assign")
    @OperLog(title = "线路标签绑定", type = BusinessType.UPDATE)
    @PutMapping("/{routeId}")
    public Result<Void> assign(@Parameter(description = "线路 ID") @PathVariable Long routeId,
                               @Valid @RequestBody RouteTagAssignRequest request) {
        tagRelService.assign(routeId, request);
        return Result.success();
    }

    @Operation(summary = "解绑单个标签")
    @SaCheckPermission("catalog:RouteTagRel:remove")
    @OperLog(title = "线路标签绑定", type = BusinessType.DELETE)
    @DeleteMapping("/{routeId}/{tagId}")
    public Result<Void> remove(@Parameter(description = "线路 ID") @PathVariable Long routeId,
                               @Parameter(description = "标签 ID") @PathVariable Long tagId) {
        tagRelService.remove(routeId, tagId);
        return Result.success();
    }
}
