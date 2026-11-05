package com.rg.trade.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import com.rg.trade.dto.PaymentQuery;
import com.rg.trade.service.PaymentRecordService;
import com.rg.trade.vo.PaymentRecordVO;
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
 * 支付记录（只读）。
 */
@Tag(name = "支付记录", description = "订单支付流水查询；支付动作请走订单支付接口")
@RestController
@RequestMapping("/trade/payment-record")
public class PaymentRecordController {

    private final PaymentRecordService paymentRecordService;

    public PaymentRecordController(PaymentRecordService paymentRecordService) {
        this.paymentRecordService = paymentRecordService;
    }

    @Operation(summary = "支付记录分页")
    @SaCheckPermission("trade:PaymentRecord:page")
    @GetMapping("/page")
    public Result<PageResult<PaymentRecordVO>> page(@Valid PaymentQuery query) {
        return Result.success(paymentRecordService.page(query));
    }

    @Operation(summary = "按订单查支付记录")
    @SaCheckPermission("trade:PaymentRecord:listByOrder")
    @GetMapping("/list/{orderId}")
    public Result<List<PaymentRecordVO>> listByOrder(@Parameter(description = "订单 ID") @PathVariable Long orderId) {
        return Result.success(paymentRecordService.listByOrder(orderId));
    }
}
