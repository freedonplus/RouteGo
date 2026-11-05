package com.rg.trade.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import com.rg.trade.dto.RefundApplyRequest;
import com.rg.trade.dto.RefundAuditRequest;
import com.rg.trade.dto.RefundQuery;
import com.rg.trade.service.RefundRecordService;
import com.rg.trade.vo.RefundRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 退款管理。
 * <p>
 * 申请与审核拆成两个权限码，便于把「提交退款」和「审批退款」分给不同角色。
 */
@Tag(name = "退款管理", description = "退款申请、审核与查询")
@RestController
@RequestMapping("/trade/refund-record")
public class RefundRecordController {

    private final RefundRecordService refundRecordService;

    public RefundRecordController(RefundRecordService refundRecordService) {
        this.refundRecordService = refundRecordService;
    }

    @Operation(summary = "退款记录分页")
    @SaCheckPermission("trade:RefundRecord:page")
    @GetMapping("/page")
    public Result<PageResult<RefundRecordVO>> page(@Valid RefundQuery query) {
        return Result.success(refundRecordService.page(query));
    }

    @Operation(summary = "退款详情")
    @SaCheckPermission("trade:RefundRecord:detail")
    @GetMapping("/{id}")
    public Result<RefundRecordVO> detail(@Parameter(description = "退款单 ID") @PathVariable Long id) {
        return Result.success(refundRecordService.detail(id));
    }

    @Operation(summary = "按订单查退款记录")
    @SaCheckPermission("trade:RefundRecord:listByOrder")
    @GetMapping("/list/{orderId}")
    public Result<List<RefundRecordVO>> listByOrder(@Parameter(description = "订单 ID") @PathVariable Long orderId) {
        return Result.success(refundRecordService.listByOrder(orderId));
    }

    @Operation(summary = "申请退款", description = "仅已支付订单可申请，申请后订单转为退款中")
    @SaCheckPermission("trade:RefundRecord:apply")
    @OperLog(title = "退款管理", type = BusinessType.INSERT)
    @PostMapping("/{orderId}")
    public Result<Long> apply(@Parameter(description = "订单 ID") @PathVariable Long orderId,
                              @Valid @RequestBody RefundApplyRequest request) {
        return Result.success(refundRecordService.apply(orderId, request));
    }

    @Operation(summary = "退款审核", description = "通过后全额退款订单转为已退款并回补库存；部分退款订单回到已支付")
    @SaCheckPermission("trade:RefundRecord:audit")
    @OperLog(title = "退款管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@Parameter(description = "退款单 ID") @PathVariable Long id,
                              @Valid @RequestBody RefundAuditRequest request) {
        refundRecordService.audit(id, request);
        return Result.success();
    }
}
