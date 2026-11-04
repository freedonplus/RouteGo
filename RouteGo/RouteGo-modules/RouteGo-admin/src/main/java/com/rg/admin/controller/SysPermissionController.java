package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.admin.dto.PermissionCreateRequest;
import com.rg.admin.dto.PermissionQuery;
import com.rg.admin.dto.PermissionUpdateRequest;
import com.rg.admin.service.SysPermissionService;
import com.rg.admin.vo.PermissionGroupVO;
import com.rg.admin.vo.SysPermissionVO;
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

/**
 * 权限点管理。
 */
@Tag(name = "权限点管理", description = "权限点 CRUD、启停与按模块分组")
@RestController
@RequestMapping("/admin/system/permission")
public class SysPermissionController {

    private final SysPermissionService permissionService;

    public SysPermissionController(SysPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "权限点分页")
    @SaCheckPermission("system:permission:list")
    @GetMapping("/page")
    public Result<PageResult<SysPermissionVO>> page(@Valid PermissionQuery query) {
        return Result.success(permissionService.page(query));
    }

    @Operation(summary = "权限点分组")
    @SaCheckPermission("system:permission:list")
    @GetMapping("/grouped")
    public Result<List<PermissionGroupVO>> grouped() {
        return Result.success(permissionService.grouped());
    }

    @Operation(summary = "权限点详情")
    @SaCheckPermission("system:permission:list")
    @GetMapping("/{id}")
    public Result<SysPermissionVO> detail(@Parameter(description = "主键 ID") @PathVariable Long id) {
        return Result.success(permissionService.detail(id));
    }

    @Operation(summary = "新增权限点")
    @SaCheckPermission("system:permission:add")
    @OperLog(title = "权限点管理", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody PermissionCreateRequest request) {
        return Result.success(permissionService.createPermission(request));
    }

    @Operation(summary = "编辑权限点")
    @SaCheckPermission("system:permission:edit")
    @OperLog(title = "权限点管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "主键 ID") @PathVariable Long id,
                               @Valid @RequestBody PermissionUpdateRequest request) {
        permissionService.updatePermission(id, request);
        return Result.success();
    }

    @Operation(summary = "删除权限点")
    @SaCheckPermission("system:permission:delete")
    @OperLog(title = "权限点管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "主键 ID") @PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用权限点")
    @SaCheckPermission("system:permission:status")
    @OperLog(title = "权限点管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "主键 ID") @PathVariable Long id, @Parameter(description = "状态：0 禁用，1 启用") @RequestParam Byte status) {
        permissionService.updateStatus(id, status);
        return Result.success();
    }
}
