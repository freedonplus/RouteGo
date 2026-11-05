package com.rg.catalog.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.catalog.dto.RouteReviewQuery;
import com.rg.catalog.dto.RouteReviewReplyRequest;
import com.rg.catalog.service.RouteReviewService;
import com.rg.catalog.vo.RouteReviewVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "线路评价", description = "评价查询、回复、显示/隐藏与删除")
@RestController
@RequestMapping("/catalog/route-review")
public class RouteReviewController {

    private final RouteReviewService reviewService;

    public RouteReviewController(RouteReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "评价分页")
    @SaCheckPermission("catalog:RouteReview:page")
    @GetMapping("/page")
    public Result<PageResult<RouteReviewVO>> page(@Valid RouteReviewQuery query) {
        return Result.success(reviewService.page(query));
    }

    @Operation(summary = "评价详情")
    @SaCheckPermission("catalog:RouteReview:detail")
    @GetMapping("/{id}")
    public Result<RouteReviewVO> detail(@Parameter(description = "评价 ID") @PathVariable Long id) {
        return Result.success(reviewService.detail(id));
    }

    @Operation(summary = "回复评价")
    @SaCheckPermission("catalog:RouteReview:reply")
    @OperLog(title = "评价管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/reply")
    public Result<Void> reply(@Parameter(description = "评价 ID") @PathVariable Long id,
                              @Valid @RequestBody RouteReviewReplyRequest request) {
        reviewService.reply(id, request);
        return Result.success();
    }

    @Operation(summary = "显示/隐藏评价")
    @SaCheckPermission("catalog:RouteReview:updateStatus")
    @OperLog(title = "评价管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "评价 ID") @PathVariable Long id,
                                     @Parameter(description = "状态：0 隐藏，1 显示") @RequestParam Byte status) {
        reviewService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除评价")
    @SaCheckPermission("catalog:RouteReview:delete")
    @OperLog(title = "评价管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "评价 ID") @PathVariable Long id) {
        reviewService.deleteReview(id);
        return Result.success();
    }
}
