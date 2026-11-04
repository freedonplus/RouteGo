package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.admin.dto.AssignIdsRequest;
import com.rg.admin.dto.RoleCreateRequest;
import com.rg.admin.dto.RoleQuery;
import com.rg.admin.dto.RoleUpdateRequest;
import com.rg.admin.service.SysRoleService;
import com.rg.admin.vo.SysRoleDetailVO;
import com.rg.admin.vo.SysRoleVO;
import com.rg.admin.vo.SysUserVO;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageQuery;
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
 * 角色管理与授权。
 */
@Tag(name = "角色管理", description = "角色 CRUD、启停、菜单/权限分配与角色用户查询")
@RestController
@RequestMapping("/admin/system/role")
public class SysRoleController {

    private final SysRoleService roleService;

    public SysRoleController(SysRoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "角色分页")
    @SaCheckPermission("system:role:list")
    @GetMapping("/page")
    public Result<PageResult<SysRoleVO>> page(@Valid RoleQuery query) {
        return Result.success(roleService.page(query));
    }

    @Operation(summary = "角色详情")
    @SaCheckPermission("system:role:list")
    @GetMapping("/{id}")
    public Result<SysRoleDetailVO> detail(@Parameter(description = "主键 ID") @PathVariable Long id) {
        return Result.success(roleService.detail(id));
    }

    @Operation(summary = "新增角色")
    @SaCheckPermission("system:role:add")
    @OperLog(title = "角色管理", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody RoleCreateRequest request) {
        return Result.success(roleService.create(request));
    }

    @Operation(summary = "编辑角色")
    @SaCheckPermission("system:role:edit")
    @OperLog(title = "角色管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "主键 ID") @PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        roleService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "启用/禁用角色")
    @SaCheckPermission("system:role:status")
    @OperLog(title = "角色管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "主键 ID") @PathVariable Long id, @Parameter(description = "状态：0 禁用，1 启用") @RequestParam Byte status) {
        roleService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @SaCheckPermission("system:role:delete")
    @OperLog(title = "角色管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "主键 ID") @PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @Operation(summary = "查询角色菜单")
    @SaCheckPermission("system:role:list")
    @GetMapping("/{id}/menus")
    public Result<List<Long>> listMenus(@Parameter(description = "主键 ID") @PathVariable Long id) {
        return Result.success(roleService.listMenuIds(id));
    }

    @Operation(summary = "分配角色菜单")
    @SaCheckPermission("system:role:assign-menu")
    @OperLog(title = "角色管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(@Parameter(description = "主键 ID") @PathVariable Long id, @Valid @RequestBody AssignIdsRequest request) {
        roleService.assignMenus(id, request);
        return Result.success();
    }

    @Operation(summary = "查询角色权限点")
    @SaCheckPermission("system:role:list")
    @GetMapping("/{id}/permissions")
    public Result<List<Long>> listPermissions(@Parameter(description = "主键 ID") @PathVariable Long id) {
        return Result.success(roleService.listPermissionIds(id));
    }

    @Operation(summary = "分配角色权限点")
    @SaCheckPermission("system:role:assign-permission")
    @OperLog(title = "角色管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/permissions")
    public Result<Void> assignPermissions(@Parameter(description = "主键 ID") @PathVariable Long id,
                                          @Valid @RequestBody AssignIdsRequest request) {
        roleService.assignPermissions(id, request);
        return Result.success();
    }

    @Operation(summary = "角色下用户分页")
    @SaCheckPermission("system:role:list")
    @GetMapping("/{id}/users")
    public Result<PageResult<SysUserVO>> pageUsers(@Parameter(description = "主键 ID") @PathVariable Long id, @Valid PageQuery pageQuery) {
        return Result.success(roleService.pageUsers(id, pageQuery));
    }
}
