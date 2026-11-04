package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.admin.dto.ConfigBatchRequest;
import com.rg.admin.dto.ConfigCreateRequest;
import com.rg.admin.dto.ConfigQuery;
import com.rg.admin.dto.ConfigUpdateRequest;
import com.rg.admin.service.SysConfigService;
import com.rg.admin.vo.SysConfigVO;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "系统参数", description = "系统参数 CRUD、按 key 查询与批量更新")
@RestController
@RequestMapping("/admin/system/config")
public class SysConfigController {

    private final SysConfigService configService;

    public SysConfigController(SysConfigService configService) {
        this.configService = configService;
    }

    @Operation(summary = "参数分页")
    @SaCheckPermission("admin:SysConfig:page")
    @GetMapping("/page")
    public Result<PageResult<SysConfigVO>> page(@Valid ConfigQuery query) {
        return Result.success(configService.page(query));
    }

    @Operation(summary = "按 key 查询参数")
    @SaCheckPermission("admin:SysConfig:getByKey")
    @GetMapping("/key/{configKey}")
    public Result<SysConfigVO> getByKey(@Parameter(description = "参数键") @PathVariable String configKey) {
        return Result.success(configService.getByKey(configKey));
    }

    @Operation(summary = "参数详情")
    @SaCheckPermission("admin:SysConfig:detail")
    @GetMapping("/{id}")
    public Result<SysConfigVO> detail(@Parameter(description = "主键 ID") @PathVariable Long id) {
        return Result.success(configService.detail(id));
    }

    @Operation(summary = "新增参数")
    @SaCheckPermission("admin:SysConfig:create")
    @OperLog(title = "参数管理", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ConfigCreateRequest request) {
        return Result.success(configService.create(request));
    }

    @Operation(summary = "编辑参数")
    @SaCheckPermission("admin:SysConfig:update")
    @OperLog(title = "参数管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "主键 ID") @PathVariable Long id,
                               @Valid @RequestBody ConfigUpdateRequest request) {
        configService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除参数")
    @SaCheckPermission("admin:SysConfig:delete")
    @OperLog(title = "参数管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "主键 ID") @PathVariable Long id) {
        configService.deleteConfig(id);
        return Result.success();
    }

    @Operation(summary = "批量更新参数")
    @SaCheckPermission("admin:SysConfig:batchUpdate")
    @OperLog(title = "参数管理", type = BusinessType.UPDATE)
    @PutMapping("/batch")
    public Result<Void> batchUpdate(@Valid @RequestBody ConfigBatchRequest request) {
        configService.batchUpdate(request);
        return Result.success();
    }
}
