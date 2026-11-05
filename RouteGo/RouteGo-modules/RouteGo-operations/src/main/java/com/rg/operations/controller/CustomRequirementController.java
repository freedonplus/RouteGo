package com.rg.operations.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import com.rg.operations.dto.CustomRequirementCreateRequest;
import com.rg.operations.dto.CustomRequirementQuery;
import com.rg.operations.dto.CustomRequirementUpdateRequest;
import com.rg.operations.service.CustomRequirementService;
import com.rg.operations.vo.CustomRequirementVO;
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

/**
 * 定制需求管理。
 * <p>
 * 需求是定制游的入口单据，状态由报价流程驱动：待跟进 → 已报价 → 已成交，
 * 人工只负责指派跟进人、确认线下成交与关闭。
 * <p>
 * 权限码规范：{@code operations:CustomRequirement:<method>}。
 */
@Tag(name = "定制需求", description = "定制游需求的受理、跟进与状态维护")
@RestController
@RequestMapping("/operations/custom-requirement")
public class CustomRequirementController {

    private final CustomRequirementService requirementService;

    public CustomRequirementController(CustomRequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @Operation(summary = "需求分页", description = "关键词模糊匹配联系人、目的地与偏好；不支持按手机号查询（密文存储无盲索引）")
    @SaCheckPermission("operations:CustomRequirement:page")
    @GetMapping("/page")
    public Result<PageResult<CustomRequirementVO>> page(@Valid CustomRequirementQuery query) {
        return Result.success(requirementService.page(query));
    }

    @Operation(summary = "需求详情", description = "附带报价版本数与最新一版报价金额")
    @SaCheckPermission("operations:CustomRequirement:detail")
    @GetMapping("/{id}")
    public Result<CustomRequirementVO> detail(@Parameter(description = "需求单 ID") @PathVariable Long id) {
        return Result.success(requirementService.detail(id));
    }

    @Operation(summary = "新建需求", description = "联系电话加密落库，出口脱敏；可关联已注册会员")
    @SaCheckPermission("operations:CustomRequirement:create")
    @OperLog(title = "新建定制需求", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CustomRequirementCreateRequest request) {
        return Result.success(requirementService.create(request));
    }

    @Operation(summary = "编辑需求", description = "已关闭的需求不可编辑；联系电话留空表示不修改")
    @SaCheckPermission("operations:CustomRequirement:update")
    @OperLog(title = "编辑定制需求", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CustomRequirementUpdateRequest request) {
        requirementService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "指派跟进人", description = "已关闭的需求不可指派")
    @SaCheckPermission("operations:CustomRequirement:assign")
    @OperLog(title = "指派定制需求", type = BusinessType.UPDATE)
    @PutMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id,
                               @Parameter(description = "跟进人（后台用户 ID）") @RequestParam Long handlerId) {
        requirementService.assign(id, handlerId);
        return Result.success();
    }

    @Operation(summary = "人工变更状态",
            description = "只接受 2 已成交（线下成交）与 3 已关闭；有报价的需求必须走采纳流程，状态 1 由报价自动驱动")
    @SaCheckPermission("operations:CustomRequirement:updateStatus")
    @OperLog(title = "定制需求状态变更", type = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Parameter(description = "目标状态：2 已成交，3 已关闭") @RequestParam Byte status,
                                     @Parameter(description = "备注，可空；非空时写入需求备注") @RequestParam(required = false) String remark) {
        requirementService.updateStatus(id, status, remark);
        return Result.success();
    }

    @Operation(summary = "删除需求", description = "已成交的需求不可删除；删除会连带清理其报价版本")
    @SaCheckPermission("operations:CustomRequirement:delete")
    @OperLog(title = "删除定制需求", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requirementService.delete(id);
        return Result.success();
    }
}
