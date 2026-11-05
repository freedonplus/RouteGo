package com.rg.operations.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import com.rg.operations.dto.CustomQuoteCreateRequest;
import com.rg.operations.dto.CustomQuoteQuery;
import com.rg.operations.dto.CustomQuoteUpdateRequest;
import com.rg.operations.service.CustomQuoteService;
import com.rg.operations.vo.CustomQuoteVO;
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
 * 定制报价管理。
 * <p>
 * 同一需求可以有多版报价，但只能有一版被采纳：采纳会自动作废同需求下的其它待确认报价，
 * 并把需求推进到已成交。
 * <p>
 * 权限码规范：{@code operations:CustomQuote:<method>}。
 */
@Tag(name = "定制报价", description = "定制游报价的创建、改价、采纳与作废")
@RestController
@RequestMapping("/operations/custom-quote")
public class CustomQuoteController {

    private final CustomQuoteService quoteService;

    public CustomQuoteController(CustomQuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Operation(summary = "报价分页", description = "可按需求、报价单号、状态筛选；expiredOnly 只看已过有效期但仍待确认的报价")
    @SaCheckPermission("operations:CustomQuote:page")
    @GetMapping("/page")
    public Result<PageResult<CustomQuoteVO>> page(@Valid CustomQuoteQuery query) {
        return Result.success(quoteService.page(query));
    }

    @Operation(summary = "报价详情")
    @SaCheckPermission("operations:CustomQuote:detail")
    @GetMapping("/{id}")
    public Result<CustomQuoteVO> detail(@Parameter(description = "报价 ID") @PathVariable Long id) {
        return Result.success(quoteService.detail(id));
    }

    @Operation(summary = "需求下的报价版本", description = "按版本从新到旧，供需求详情页的报价时间线使用")
    @SaCheckPermission("operations:CustomQuote:listByRequirement")
    @GetMapping("/list")
    public Result<List<CustomQuoteVO>> listByRequirement(
            @Parameter(description = "需求单 ID") @RequestParam Long requirementId) {
        return Result.success(quoteService.listByRequirement(requirementId));
    }

    @Operation(summary = "新建报价", description = "报价成功后需求若还是待跟进，会自动推进到已报价")
    @SaCheckPermission("operations:CustomQuote:create")
    @OperLog(title = "新建定制报价", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CustomQuoteCreateRequest request) {
        return Result.success(quoteService.create(request));
    }

    @Operation(summary = "修改报价", description = "仅待确认的报价可改；调价请新建一版，不要改已采纳的报价")
    @SaCheckPermission("operations:CustomQuote:update")
    @OperLog(title = "修改定制报价", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CustomQuoteUpdateRequest request) {
        quoteService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "采纳报价", description = "本单置已采纳、同需求其它待确认报价置已失效、需求置已成交")
    @SaCheckPermission("operations:CustomQuote:accept")
    @OperLog(title = "采纳定制报价", type = BusinessType.UPDATE)
    @PutMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        quoteService.accept(id);
        return Result.success();
    }

    @Operation(summary = "作废报价", description = "仅待确认的报价可作废；已采纳的报价不能作废")
    @SaCheckPermission("operations:CustomQuote:invalidate")
    @OperLog(title = "作废定制报价", type = BusinessType.UPDATE)
    @PutMapping("/{id}/invalidate")
    public Result<Void> invalidate(@PathVariable Long id) {
        quoteService.invalidate(id);
        return Result.success();
    }

    @Operation(summary = "删除报价", description = "仅待确认的报价可删除，物理删除不可恢复")
    @SaCheckPermission("operations:CustomQuote:delete")
    @OperLog(title = "删除定制报价", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        quoteService.delete(id);
        return Result.success();
    }
}
