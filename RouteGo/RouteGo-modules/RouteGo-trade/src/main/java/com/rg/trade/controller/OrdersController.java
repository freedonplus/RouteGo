package com.rg.trade.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import com.rg.trade.dto.OrderCancelRequest;
import com.rg.trade.dto.OrderCreateRequest;
import com.rg.trade.dto.OrderPayRequest;
import com.rg.trade.dto.OrderQuery;
import com.rg.trade.dto.OrderUpdateRequest;
import com.rg.trade.service.OrderService;
import com.rg.trade.support.OrderCreateFacade;
import com.rg.trade.vo.OrderVO;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单管理。
 * <p>
 * 状态流转接口按动作拆分（支付/取消/开始行程/完成），而不是让前端直接改 status，
 * 这样服务端能保证每次流转的合法性校验与连带更新一定被执行。
 * <p>
 * 权限码规范：{@code trade:<Controller>:<method>}。
 */
@Tag(name = "订单管理", description = "订单查询、代录下单、支付、取消、行程流转与删除")
@RestController
@RequestMapping("/trade/orders")
public class OrdersController {

    private final OrderService orderService;
    private final OrderCreateFacade orderCreateFacade;

    public OrdersController(OrderService orderService, OrderCreateFacade orderCreateFacade) {
        this.orderService = orderService;
        this.orderCreateFacade = orderCreateFacade;
    }

    @Operation(summary = "订单分页", description = "支持按订单号/客户/线路/状态/出发日期/下单时间筛选；联系电话按盲索引精确匹配")
    @SaCheckPermission("trade:Orders:page")
    @GetMapping("/page")
    public Result<PageResult<OrderVO>> page(@Valid OrderQuery query) {
        return Result.success(orderService.page(query));
    }

    @Operation(summary = "订单详情", description = "同时返回出行人、状态流水、支付记录与退款记录")
    @SaCheckPermission("trade:Orders:detail")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@Parameter(description = "订单 ID") @PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    @Operation(summary = "后台代录订单",
            description = "按当前线路与班期生成快照并占用库存，初始状态为待支付；"
                    + "可传 Idempotency-Key 头防止重复提交")
    @SaCheckPermission("trade:Orders:create")
    @OperLog(title = "订单管理", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody OrderCreateRequest request,
                               @Parameter(description = "幂等键，同键重复提交返回同一张订单")
                               @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return Result.success(orderCreateFacade.create(request, idempotencyKey));
    }

    @Operation(summary = "修改订单联系信息", description = "金额、线路与出发日期为快照，不可修改")
    @SaCheckPermission("trade:Orders:update")
    @OperLog(title = "订单管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "订单 ID") @PathVariable Long id,
                               @Valid @RequestBody OrderUpdateRequest request) {
        orderService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "取消订单", description = "仅待支付订单可取消，取消后自动回补班期库存")
    @SaCheckPermission("trade:Orders:cancel")
    @OperLog(title = "订单管理", type = BusinessType.UPDATE)
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@Parameter(description = "订单 ID") @PathVariable Long id,
                               @Valid @RequestBody OrderCancelRequest request) {
        orderService.cancel(id, request);
        return Result.success();
    }

    @Operation(summary = "订单支付", description = "按订单实付金额收款，成功后累加线路销量与客户消费")
    @SaCheckPermission("trade:Orders:pay")
    @OperLog(title = "订单管理", type = BusinessType.UPDATE)
    @PostMapping("/{id}/pay")
    public Result<Void> pay(@Parameter(description = "订单 ID") @PathVariable Long id,
                            @RequestBody(required = false) OrderPayRequest request) {
        orderService.pay(id, request == null ? new OrderPayRequest() : request);
        return Result.success();
    }

    @Operation(summary = "开始行程", description = "已支付 → 出行中")
    @SaCheckPermission("trade:Orders:startTravel")
    @OperLog(title = "订单管理", type = BusinessType.UPDATE)
    @PostMapping("/{id}/start")
    public Result<Void> startTravel(@Parameter(description = "订单 ID") @PathVariable Long id) {
        orderService.startTravel(id);
        return Result.success();
    }

    @Operation(summary = "完成行程", description = "出行中 → 已完成")
    @SaCheckPermission("trade:Orders:complete")
    @OperLog(title = "订单管理", type = BusinessType.UPDATE)
    @PostMapping("/{id}/complete")
    public Result<Void> complete(@Parameter(description = "订单 ID") @PathVariable Long id) {
        orderService.complete(id);
        return Result.success();
    }

    @Operation(summary = "删除订单", description = "仅已完成/已取消/已退款订单可删除（逻辑删除）")
    @SaCheckPermission("trade:Orders:delete")
    @OperLog(title = "订单管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "订单 ID") @PathVariable Long id) {
        orderService.delete(id);
        return Result.success();
    }
}
