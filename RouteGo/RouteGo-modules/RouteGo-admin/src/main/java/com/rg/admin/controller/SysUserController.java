package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.admin.dto.AssignRolesRequest;
import com.rg.admin.dto.ResetPasswordRequest;
import com.rg.admin.dto.UserCreateRequest;
import com.rg.admin.dto.UserQuery;
import com.rg.admin.dto.UserUpdateRequest;
import com.rg.admin.service.SysUserService;
import com.rg.admin.vo.SysUserDetailVO;
import com.rg.admin.vo.SysUserVO;
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
 * 系统用户管理。
 */
@Tag(name = "系统用户", description = "后台用户的增删改查、启停、重置密码与角色分配")
@RestController
@RequestMapping("/admin/system/user")
public class SysUserController {

    private final SysUserService userService;

    public SysUserController(SysUserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户分页")
    @SaCheckPermission("system:user:list")
    @GetMapping("/page")
    public Result<PageResult<SysUserVO>> page(@Valid UserQuery query) {
        return Result.success(userService.page(query));
    }

    @Operation(summary = "用户详情")
    @SaCheckPermission("system:user:list")
    @GetMapping("/{id}")
    public Result<SysUserDetailVO> detail(@Parameter(description = "主键 ID") @PathVariable Long id) {
        return Result.success(userService.detail(id));
    }

    @Operation(summary = "新增用户")
    @SaCheckPermission("system:user:add")
    @OperLog(title = "用户管理", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.create(request));
    }

    @Operation(summary = "编辑用户")
    @SaCheckPermission("system:user:edit")
    @OperLog(title = "用户管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "主键 ID") @PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        userService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "启用/禁用用户")
    @SaCheckPermission("system:user:status")
    @OperLog(title = "用户管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "主键 ID") @PathVariable Long id, @Parameter(description = "状态：0 禁用，1 启用") @RequestParam Byte status) {
        userService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "重置用户密码")
    @SaCheckPermission("system:user:reset-password")
    @OperLog(title = "用户管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@Parameter(description = "主键 ID") @PathVariable Long id,
                                      @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return Result.success();
    }

    @Operation(summary = "查询用户角色")
    @SaCheckPermission("system:user:list")
    @GetMapping("/{id}/roles")
    public Result<List<Long>> listRoles(@Parameter(description = "主键 ID") @PathVariable Long id) {
        return Result.success(userService.listRoleIds(id));
    }

    @Operation(summary = "分配用户角色")
    @SaCheckPermission("system:user:assign-role")
    @OperLog(title = "用户管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@Parameter(description = "主键 ID") @PathVariable Long id,
                                    @Valid @RequestBody AssignRolesRequest request) {
        userService.assignRoles(id, request);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @SaCheckPermission("system:user:delete")
    @OperLog(title = "用户管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "主键 ID") @PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
