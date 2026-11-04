package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.admin.dto.DictDataCreateRequest;
import com.rg.admin.dto.DictDataQuery;
import com.rg.admin.dto.DictDataUpdateRequest;
import com.rg.admin.service.SysDictDataService;
import com.rg.admin.vo.DictDataVO;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "字典数据", description = "字典数据 CRUD、按类型查询、默认项与启停")
@RestController
@RequestMapping("/admin/system/dict/data")
public class SysDictDataController {

    private final SysDictDataService dictDataService;

    public SysDictDataController(SysDictDataService dictDataService) {
        this.dictDataService = dictDataService;
    }

    @Operation(summary = "字典数据分页")
    @SaCheckPermission("system:dict:list")
    @GetMapping("/page")
    public Result<PageResult<DictDataVO>> page(@Valid DictDataQuery query) {
        return Result.success(dictDataService.page(query));
    }

    @Operation(summary = "按类型获取启用的字典数据")
    @SaCheckLogin
    @GetMapping("/list/{dictType}")
    public Result<List<DictDataVO>> listByType(@PathVariable String dictType) {
        return Result.success(dictDataService.listByType(dictType));
    }

    @Operation(summary = "字典数据详情")
    @SaCheckPermission("system:dict:list")
    @GetMapping("/{id}")
    public Result<DictDataVO> detail(@PathVariable Long id) {
        return Result.success(dictDataService.detail(id));
    }

    @Operation(summary = "新增字典数据")
    @SaCheckPermission("system:dict:add")
    @OperLog(title = "字典管理", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody DictDataCreateRequest request) {
        return Result.success(dictDataService.create(request));
    }

    @Operation(summary = "编辑字典数据")
    @SaCheckPermission("system:dict:edit")
    @OperLog(title = "字典管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody DictDataUpdateRequest request) {
        dictDataService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除字典数据")
    @SaCheckPermission("system:dict:delete")
    @OperLog(title = "字典管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dictDataService.deleteData(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用字典数据")
    @SaCheckPermission("system:dict:edit")
    @OperLog(title = "字典管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Byte status) {
        dictDataService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "设为默认项")
    @SaCheckPermission("system:dict:edit")
    @OperLog(title = "字典管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        dictDataService.setDefault(id);
        return Result.success();
    }
}
