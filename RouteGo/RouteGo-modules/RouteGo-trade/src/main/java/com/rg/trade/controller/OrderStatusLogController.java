package com.rg.trade.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import com.rg.trade.dto.OrderStatusLogQuery;
import com.rg.trade.service.OrderStatusLogService;
import com.rg.trade.vo.OrderStatusLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单状态流水（只读）。
 */
@Tag(name = "订单状态流水", description = "订单状态变更的追加型台账，不提供修改与删除")
@RestController
@RequestMapping("/trade/order-status-log")
public class OrderStatusLogController {

    private final OrderStatusLogService statusLogService;

    public OrderStatusLogController(OrderStatusLogService statusLogService) {
        this.statusLogService = statusLogService;
    }

    @Operation(summary = "状态流水分页")
    @SaCheckPermission("trade:OrderStatusLog:page")
    @GetMapping("/page")
    public Result<PageResult<OrderStatusLogVO>> page(@Valid OrderStatusLogQuery query) {
        return Result.success(statusLogService.page(query));
    }

    @Operation(summary = "按订单查流水", description = "按时间正序，第一条为下单")
    @SaCheckPermission("trade:OrderStatusLog:listByOrder")
    @GetMapping("/list/{orderId}")
    public Result<List<OrderStatusLogVO>> listByOrder(@Parameter(description = "订单 ID") @PathVariable Long orderId) {
        return Result.success(statusLogService.listByOrder(orderId));
    }
}
