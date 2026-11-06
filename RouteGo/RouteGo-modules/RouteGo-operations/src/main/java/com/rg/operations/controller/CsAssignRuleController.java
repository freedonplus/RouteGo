package com.rg.operations.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import com.rg.operations.dto.CsAssignRuleQuery;
import com.rg.operations.dto.CsAssignRuleSaveRequest;
import com.rg.operations.service.CsAssignRuleService;
import com.rg.operations.support.CsAssignEngine;
import com.rg.operations.vo.AssignPreviewVO;
import com.rg.operations.vo.CsAssignRuleVO;
import com.rg.operations.vo.HandlerWorkloadVO;
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
 * 客服自动分配规则管理。
 * <p>
 * 一张工单只会命中一条规则：按 sort 升序找第一条「类型与优先级都匹配」的启用规则。
 * 想看现在的配置会怎么分，用 {@code /preview} 试算，不用真造一张工单。
 * <p>
 * 权限码规范：{@code operations:CsAssignRule:<method>}。
 */
@Tag(name = "客服自动分配规则", description = "按工单类型与优先级把新工单分配给指定范围的客服")
@RestController
@RequestMapping("/operations/cs-assign-rule")
public class CsAssignRuleController {

    private final CsAssignRuleService ruleService;
    private final CsAssignEngine assignEngine;

    public CsAssignRuleController(CsAssignRuleService ruleService, CsAssignEngine assignEngine) {
        this.ruleService = ruleService;
        this.assignEngine = assignEngine;
    }

    @Operation(summary = "规则分页", description = "按匹配顺序返回，数字小的先命中")
    @SaCheckPermission("operations:CsAssignRule:page")
    @GetMapping("/page")
    public Result<PageResult<CsAssignRuleVO>> page(@Valid CsAssignRuleQuery query) {
        return Result.success(ruleService.page(query));
    }

    @Operation(summary = "启用中的规则", description = "按匹配顺序返回，用于展示当前生效顺序")
    @SaCheckPermission("operations:CsAssignRule:page")
    @GetMapping("/enabled")
    public Result<List<CsAssignRuleVO>> enabled() {
        return Result.success(ruleService.listEnabled());
    }

    @Operation(summary = "规则详情")
    @SaCheckPermission("operations:CsAssignRule:page")
    @GetMapping("/{id}")
    public Result<CsAssignRuleVO> detail(@Parameter(description = "规则 ID") @PathVariable Long id) {
        return Result.success(ruleService.detail(id));
    }

    @Operation(summary = "新建规则")
    @SaCheckPermission("operations:CsAssignRule:create")
    @OperLog(title = "客服自动分配规则", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CsAssignRuleSaveRequest request) {
        return Result.success(ruleService.create(request));
    }

    @Operation(summary = "编辑规则")
    @SaCheckPermission("operations:CsAssignRule:update")
    @OperLog(title = "客服自动分配规则", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody CsAssignRuleSaveRequest request) {
        ruleService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "启停规则", description = "停用后不再参与新工单分配，已分配的工单不受影响")
    @SaCheckPermission("operations:CsAssignRule:updateStatus")
    @OperLog(title = "客服自动分配规则", type = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Parameter(description = "0 停用，1 启用") @RequestParam Integer enabled) {
        ruleService.updateStatus(id, enabled);
        return Result.success();
    }

    @Operation(summary = "删除规则", description = "逻辑删除；历史工单上的分配记录仍可追溯")
    @SaCheckPermission("operations:CsAssignRule:delete")
    @OperLog(title = "客服自动分配规则", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ruleService.delete(id);
        return Result.success();
    }

    @Operation(summary = "客服在手工单量", description = "配置「在处理最少」策略时参考，避免把单压给已经忙不过来的人")
    @SaCheckPermission("operations:CsAssignRule:page")
    @GetMapping("/workload")
    public Result<List<HandlerWorkloadVO>> workload(
            @Parameter(description = "客服 ID，逗号分隔") @RequestParam List<Long> userIds) {
        return Result.success(ruleService.workload(userIds));
    }

    @Operation(summary = "分配试算", description = "给一个工单类型与优先级，返回会命中的规则与客服，不产生工单")
    @SaCheckPermission("operations:CsAssignRule:page")
    @GetMapping("/preview")
    public Result<AssignPreviewVO> preview(
            @Parameter(description = "工单类型：1 售前，2 订单，3 退款，4 投诉，5 其他") @RequestParam Byte type,
            @Parameter(description = "优先级：1 低，2 中，3 高；默认 2") @RequestParam(required = false) Byte priority) {
        return Result.success(assignEngine.preview(type, priority == null ? (byte) 2 : priority));
    }
}
