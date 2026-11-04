package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.admin.dto.DictTypeCreateRequest;
import com.rg.admin.dto.DictTypeQuery;
import com.rg.admin.dto.DictTypeUpdateRequest;
import com.rg.admin.service.SysDictTypeService;
import com.rg.admin.vo.DictTypeVO;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "字典类型", description = "字典类型 CRUD 与启停")
@RestController
@RequestMapping("/admin/system/dict/type")
public class SysDictTypeController {

    private final SysDictTypeService dictTypeService;

    public SysDictTypeController(SysDictTypeService dictTypeService) {
        this.dictTypeService = dictTypeService;
    }

    @Operation(summary = "字典类型分页")
    @SaCheckPermission("system:dict:list")
    @GetMapping("/page")
    public Result<PageResult<DictTypeVO>> page(@Valid DictTypeQuery query) {
        return Result.success(dictTypeService.page(query));
    }

    @Operation(summary = "字典类型详情")
    @SaCheckPermission("system:dict:list")
    @GetMapping("/{id}")
    public Result<DictTypeVO> detail(@Parameter(description = "主键 ID") @PathVariable Long id) {
        return Result.success(dictTypeService.detail(id));
    }

    @Operation(summary = "新增字典类型")
    @SaCheckPermission("system:dict:add")
    @OperLog(title = "字典管理", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody DictTypeCreateRequest request) {
        return Result.success(dictTypeService.create(request));
    }

    @Operation(summary = "编辑字典类型")
    @SaCheckPermission("system:dict:edit")
    @OperLog(title = "字典管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "主键 ID") @PathVariable Long id,
                               @Valid @RequestBody DictTypeUpdateRequest request) {
        dictTypeService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除字典类型")
    @SaCheckPermission("system:dict:delete")
    @OperLog(title = "字典管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "主键 ID") @PathVariable Long id) {
        dictTypeService.deleteType(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用字典类型")
    @SaCheckPermission("system:dict:edit")
    @OperLog(title = "字典管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "主键 ID") @PathVariable Long id, @Parameter(description = "状态：0 禁用，1 启用") @RequestParam Byte status) {
        dictTypeService.updateStatus(id, status);
        return Result.success();
    }
}
