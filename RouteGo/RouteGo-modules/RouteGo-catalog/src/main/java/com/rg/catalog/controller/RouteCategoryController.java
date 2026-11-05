package com.rg.catalog.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.catalog.dto.RouteCategorySaveRequest;
import com.rg.catalog.service.RouteCategoryService;
import com.rg.catalog.vo.RouteCategoryVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "线路分类", description = "线路分类树与增删改")
@RestController
@RequestMapping("/catalog/route-category")
public class RouteCategoryController {

    private final RouteCategoryService categoryService;

    public RouteCategoryController(RouteCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "分类树", description = "返回按 sort 排序的完整分类树")
    @SaCheckPermission("catalog:RouteCategory:tree")
    @GetMapping("/tree")
    public Result<List<RouteCategoryVO>> tree() {
        return Result.success(categoryService.tree());
    }

    @Operation(summary = "分类详情")
    @SaCheckPermission("catalog:RouteCategory:detail")
    @GetMapping("/{id}")
    public Result<RouteCategoryVO> detail(@Parameter(description = "分类 ID") @PathVariable Long id) {
        return Result.success(categoryService.detail(id));
    }

    @Operation(summary = "新增分类")
    @SaCheckPermission("catalog:RouteCategory:create")
    @OperLog(title = "线路分类", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody RouteCategorySaveRequest request) {
        return Result.success(categoryService.create(request));
    }

    @Operation(summary = "编辑分类", description = "父级不能是自己或自己的子分类")
    @SaCheckPermission("catalog:RouteCategory:update")
    @OperLog(title = "线路分类", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "分类 ID") @PathVariable Long id,
                               @Valid @RequestBody RouteCategorySaveRequest request) {
        categoryService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "启用/禁用分类")
    @SaCheckPermission("catalog:RouteCategory:updateStatus")
    @OperLog(title = "线路分类", type = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "分类 ID") @PathVariable Long id,
                                     @Parameter(description = "状态：0 禁用，1 启用") @RequestParam Byte status) {
        categoryService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除分类", description = "存在子分类或线路时不允许删除")
    @SaCheckPermission("catalog:RouteCategory:delete")
    @OperLog(title = "线路分类", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "分类 ID") @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }
}
