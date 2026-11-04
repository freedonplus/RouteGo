package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.admin.dto.UserOauthBindRequest;
import com.rg.admin.service.SysUserOauthService;
import com.rg.admin.vo.SysUserOauthVO;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "第三方绑定", description = "后台用户第三方账号绑定与解绑")
@RestController
@RequestMapping("/admin/system/user-oauth")
public class SysUserOauthController {

    private final SysUserOauthService oauthService;

    public SysUserOauthController(SysUserOauthService oauthService) {
        this.oauthService = oauthService;
    }

    @Operation(summary = "用户绑定列表")
    @SaCheckPermission("system:user:list")
    @GetMapping("/user/{userId}")
    public Result<List<SysUserOauthVO>> listByUser(@Parameter(description = "用户 ID") @PathVariable Long userId) {
        return Result.success(oauthService.listByUserId(userId));
    }

    @Operation(summary = "绑定第三方账号")
    @SaCheckPermission("system:user:edit")
    @OperLog(title = "第三方绑定", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> bind(@Valid @RequestBody UserOauthBindRequest request) {
        return Result.success(oauthService.bind(request));
    }

    @Operation(summary = "解绑第三方账号")
    @SaCheckPermission("system:user:edit")
    @OperLog(title = "第三方绑定", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> unbind(@Parameter(description = "主键 ID") @PathVariable Long id) {
        oauthService.unbind(id);
        return Result.success();
    }
}
