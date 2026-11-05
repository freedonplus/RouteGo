package com.rg.customer.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import com.rg.customer.dto.CouponQuery;
import com.rg.customer.dto.CouponUseRequest;
import com.rg.customer.service.CouponService;
import com.rg.customer.vo.CouponVO;
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
 * 用户优惠券管理。
 * <p>
 * 券的过期时间由模板规则实时推导，展示时一并返回是否可用；
 * 核销是条件更新，重复核销与并发核销都会拿到 409。
 * <p>
 * 权限码规范：{@code customer:<Controller>:<method>}。
 */
@Tag(name = "用户优惠券", description = "券查询、会员券包、核销、作废与过期刷新")
@RestController
@RequestMapping("/customer/coupon")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @Operation(summary = "优惠券分页", description = "可按会员、模板、券码、状态与领取时间筛选")
    @SaCheckPermission("customer:Coupon:page")
    @GetMapping("/page")
    public Result<PageResult<CouponVO>> page(@Valid CouponQuery query) {
        return Result.success(couponService.page(query));
    }

    @Operation(summary = "优惠券详情")
    @SaCheckPermission("customer:Coupon:detail")
    @GetMapping("/{id}")
    public Result<CouponVO> detail(@Parameter(description = "券 ID") @PathVariable Long id) {
        return Result.success(couponService.detail(id));
    }

    @Operation(summary = "按会员查券", description = "会员券包视图，可按状态过滤")
    @SaCheckPermission("customer:Coupon:list")
    @GetMapping("/list/{customerId}")
    public Result<List<CouponVO>> listByCustomer(@PathVariable Long customerId,
                                                 @Parameter(description = "0 未使用，1 已使用，2 已过期")
                                                 @RequestParam(required = false) Byte status) {
        return Result.success(couponService.listByCustomer(customerId, status));
    }

    @Operation(summary = "核销优惠券",
            description = "传订单 ID 时校验订单归属、状态与使用门槛；已使用或已过期返回 409")
    @SaCheckPermission("customer:Coupon:use")
    @OperLog(title = "核销优惠券", type = BusinessType.UPDATE)
    @PutMapping("/{id}/use")
    public Result<Void> use(@PathVariable Long id, @RequestBody(required = false) CouponUseRequest request) {
        couponService.use(id, request);
        return Result.success();
    }

    @Operation(summary = "作废优惠券", description = "仅未使用的券可作废；已使用的券请走订单退款流程")
    @SaCheckPermission("customer:Coupon:revoke")
    @OperLog(title = "作废优惠券", type = BusinessType.UPDATE)
    @PutMapping("/{id}/revoke")
    public Result<Void> revoke(@PathVariable Long id) {
        couponService.revoke(id);
        return Result.success();
    }

    @Operation(summary = "删除优惠券记录", description = "已使用的券是核销凭证，不允许删除")
    @SaCheckPermission("customer:Coupon:delete")
    @OperLog(title = "删除优惠券", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return Result.success();
    }

    @Operation(summary = "刷新过期状态",
            description = "按模板有效期规则，把已过期但仍标记为未使用的券批量置为「已过期」；返回受影响数量")
    @SaCheckPermission("customer:Coupon:refreshExpired")
    @OperLog(title = "刷新优惠券过期状态", type = BusinessType.UPDATE)
    @PostMapping("/expire-refresh")
    public Result<Integer> refreshExpired() {
        return Result.success(couponService.refreshExpired());
    }
}
