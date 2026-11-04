package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.rg.admin.dto.MenuCreateRequest;
import com.rg.admin.dto.MenuUpdateRequest;
import com.rg.admin.service.SysMenuService;
import com.rg.admin.vo.MenuNodeVO;
import com.rg.admin.vo.SysMenuVO;
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

/**
 * 菜单管理。
 */
@Tag(name = "菜单管理", description = "菜单树 CRUD、启停、显隐与当前用户菜单")
@RestController
@RequestMapping("/admin/system/menu")
public class SysMenuController {

    private final SysMenuService menuService;

    public SysMenuController(SysMenuService menuService) {
        this.menuService = menuService;
    }

    @Operation(summary = "菜单树")
    @SaCheckPermission("system:menu:list")
    @GetMapping("/tree")
    public Result<List<MenuNodeVO>> tree() {
        return Result.success(menuService.tree());
    }

    @Operation(summary = "当前用户菜单树")
    @SaCheckLogin
    @GetMapping("/user-tree")
    public Result<List<MenuNodeVO>> userTree() {
        return Result.success(menuService.userTree(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "菜单详情")
    @SaCheckPermission("system:menu:list")
    @GetMapping("/{id}")
    public Result<SysMenuVO> detail(@Parameter(description = "主键 ID") @PathVariable Long id) {
        return Result.success(menuService.detail(id));
    }

    @Operation(summary = "新增菜单")
    @SaCheckPermission("system:menu:add")
    @OperLog(title = "菜单管理", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody MenuCreateRequest request) {
        return Result.success(menuService.createMenu(request));
    }

    @Operation(summary = "编辑菜单")
    @SaCheckPermission("system:menu:edit")
    @OperLog(title = "菜单管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "主键 ID") @PathVariable Long id, @Valid @RequestBody MenuUpdateRequest request) {
        menuService.updateMenu(id, request);
        return Result.success();
    }

    @Operation(summary = "删除菜单")
    @SaCheckPermission("system:menu:delete")
    @OperLog(title = "菜单管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "主键 ID") @PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用菜单")
    @SaCheckPermission("system:menu:status")
    @OperLog(title = "菜单管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "主键 ID") @PathVariable Long id, @Parameter(description = "状态：0 禁用，1 启用") @RequestParam Byte status) {
        menuService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "显示/隐藏菜单")
    @SaCheckPermission("system:menu:edit")
    @OperLog(title = "菜单管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/visible")
    public Result<Void> updateVisible(@Parameter(description = "主键 ID") @PathVariable Long id, @Parameter(description = "是否显示：0 隐藏，1 显示") @RequestParam Byte visible) {
        menuService.updateVisible(id, visible);
        return Result.success();
    }
}
