package com.rg.trade.controller.app;

import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.BusinessException;
import com.rg.common.security.StpKit;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import com.rg.trade.dto.OrderCancelRequest;
import com.rg.trade.dto.OrderCreateRequest;
import com.rg.trade.dto.OrderPayRequest;
import com.rg.trade.dto.OrderQuery;
import com.rg.trade.dto.app.AppOrderCreateRequest;
import com.rg.trade.service.OrderService;
import com.rg.trade.service.OrderTravelerService;
import com.rg.trade.support.OrderCreateFacade;
import com.rg.trade.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * C 端订单。
 * <p>
 * 全部接口要求会员登录，且强制按登录态过滤归属：会员只能看到和操作自己的订单，
 * 客户端传来的任何 customerId 一律忽略。业务逻辑全部复用 {@link OrderService}。
 */
@Slf4j
@Tag(name = "App-订单", description = "下单 / 我的订单 / 取消 / 支付")
@RestController
@RequestMapping("/app/orders")
public class AppOrderController {

    /** App 渠道来源，与后台代录（1）区分 */
    private static final byte SOURCE_APP = 2;

    private final OrderService orderService;
    private final OrderTravelerService orderTravelerService;
    private final OrderCreateFacade orderCreateFacade;

    public AppOrderController(OrderService orderService,
                              OrderTravelerService orderTravelerService,
                              OrderCreateFacade orderCreateFacade) {
        this.orderService = orderService;
        this.orderTravelerService = orderTravelerService;
        this.orderCreateFacade = orderCreateFacade;
    }

    @Operation(summary = "创建订单",
            description = "会员下单；客户身份取登录态，无需传 customerId。"
                    + "建议前端每次提交生成 UUID 放在 Idempotency-Key 头，重复提交会返回同一张订单。")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AppOrderCreateRequest request,
                               @Parameter(description = "幂等键，同键重复提交返回同一张订单")
                               @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        OrderCreateRequest create = new OrderCreateRequest();
        create.setCustomerId(requireCustomer());
        create.setRouteId(request.getRouteId());
        create.setDepartureId(request.getDepartureId());
        create.setTravelerCount(request.getTravelerCount());
        create.setContactName(request.getContactName());
        create.setContactPhone(request.getContactPhone());
        create.setRemark(request.getRemark());
        create.setSource(SOURCE_APP);
        create.setTravelers(request.getTravelers());
        return Result.success(orderCreateFacade.create(create, idempotencyKey));
    }

    @Operation(summary = "我的订单", description = "只返回当前登录会员的订单；status 不传为全部。")
    @GetMapping
    public Result<PageResult<OrderVO>> page(@Parameter(description = "订单状态：0 待支付 1 已支付 2 出行中 3 已完成 4 已取消")
                                            @RequestParam(required = false) Byte status,
                                            @RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        OrderQuery query = new OrderQuery();
        query.setCustomerId(requireCustomer());
        query.setStatus(status);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        return Result.success(orderService.page(query));
    }

    @Operation(summary = "订单详情", description = "含出行人；非本人订单返回 403。")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@Parameter(description = "订单 ID") @PathVariable Long id) {
        OrderVO order = requireOwnOrder(id);
        order.setTravelers(orderTravelerService.listByOrder(id));
        return Result.success(order);
    }

    @Operation(summary = "取消订单", description = "仅待支付订单可取消，取消后释放班期库存。")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@Parameter(description = "订单 ID") @PathVariable Long id,
                               @RequestBody(required = false) OrderCancelRequest request) {
        requireOwnOrder(id);
        OrderCancelRequest cancel = request == null ? new OrderCancelRequest() : request;
        if (cancel.getReason() == null || cancel.getReason().isBlank()) {
            cancel.setReason("用户主动取消");
        }
        orderService.cancel(id, cancel);
        return Result.success();
    }

    @Operation(summary = "支付订单",
            description = "站内支付（演示用）：直接把订单置为已支付并写支付流水。"
                    + "接入微信支付时改为返回预支付参数，由支付回调驱动 OrderService.pay。")
    @PostMapping("/{id}/pay")
    public Result<Void> pay(@Parameter(description = "订单 ID") @PathVariable Long id,
                            @RequestBody(required = false) OrderPayRequest request) {
        requireOwnOrder(id);
        OrderPayRequest pay = request == null ? new OrderPayRequest() : request;
        if (pay.getPayType() == null) {
            pay.setPayType((byte) 1);
        }
        if (pay.getRemark() == null || pay.getRemark().isBlank()) {
            pay.setRemark("App 站内支付");
        }
        orderService.pay(id, pay);
        return Result.success();
    }

    private Long requireCustomer() {
        StpKit.CUSTOMER.checkLogin();
        return StpKit.currentCustomerId();
    }

    private OrderVO requireOwnOrder(Long orderId) {
        Long customerId = requireCustomer();
        OrderVO order = orderService.detail(orderId);
        if (!Objects.equals(order.getCustomerId(), customerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该订单");
        }
        return order;
    }
}
