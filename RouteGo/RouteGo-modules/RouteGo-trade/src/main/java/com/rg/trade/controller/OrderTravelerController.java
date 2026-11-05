package com.rg.trade.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.trade.dto.OrderTravelerBatchRequest;
import com.rg.trade.dto.OrderTravelerSaveRequest;
import com.rg.trade.service.OrderTravelerService;
import com.rg.trade.vo.OrderTravelerVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单出行人管理。
 * <p>
 * 证件号与手机号加密落库，接口一律返回脱敏结果；终态订单不允许再改名单。
 */
@Tag(name = "订单出行人", description = "出行人名单维护（证件号与手机号加密存储）")
@RestController
@RequestMapping("/trade/order-traveler")
public class OrderTravelerController {

    private final OrderTravelerService travelerService;

    public OrderTravelerController(OrderTravelerService travelerService) {
        this.travelerService = travelerService;
    }

    @Operation(summary = "按订单查出出行人")
    @SaCheckPermission("trade:OrderTraveler:listByOrder")
    @GetMapping("/list/{orderId}")
    public Result<List<OrderTravelerVO>> listByOrder(@Parameter(description = "订单 ID") @PathVariable Long orderId) {
        return Result.success(travelerService.listByOrder(orderId));
    }

    @Operation(summary = "新增出行人")
    @SaCheckPermission("trade:OrderTraveler:create")
    @OperLog(title = "订单出行人", type = BusinessType.INSERT)
    @PostMapping("/{orderId}")
    public Result<Long> create(@Parameter(description = "订单 ID") @PathVariable Long orderId,
                               @Valid @RequestBody OrderTravelerSaveRequest request) {
        return Result.success(travelerService.create(orderId, request));
    }

    @Operation(summary = "修改出行人")
    @SaCheckPermission("trade:OrderTraveler:update")
    @OperLog(title = "订单出行人", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "出行人 ID") @PathVariable Long id,
                               @Valid @RequestBody OrderTravelerSaveRequest request) {
        travelerService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除出行人")
    @SaCheckPermission("trade:OrderTraveler:delete")
    @OperLog(title = "订单出行人", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "出行人 ID") @PathVariable Long id) {
        travelerService.delete(id);
        return Result.success();
    }

    @Operation(summary = "覆盖保存整单出行人", description = "先清空该订单现有名单，再按本次入参重建")
    @SaCheckPermission("trade:OrderTraveler:replaceAll")
    @OperLog(title = "订单出行人", type = BusinessType.UPDATE)
    @PutMapping("/batch/{orderId}")
    public Result<Void> replaceAll(@Parameter(description = "订单 ID") @PathVariable Long orderId,
                                   @Valid @RequestBody OrderTravelerBatchRequest request) {
        travelerService.replaceAll(orderId, request.getItems());
        return Result.success();
    }
}
