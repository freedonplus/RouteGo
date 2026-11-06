package com.rg.customer.controller.app;

import com.rg.common.security.StpKit;
import com.rg.common.util.Result;
import com.rg.customer.service.CouponService;
import com.rg.customer.vo.CouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端券包（只读）。
 * <p>
 * 后台的 {@code /customer/coupon/**} 带 {@code @SaCheckPermission}，App 会员没有后台角色，
 * 不能直接复用；这里按登录态暴露自己的券，逻辑仍走 {@link CouponService}。
 */
@Tag(name = "App-优惠券", description = "我的券包：可用 / 已使用 / 已过期")
@RestController
@RequestMapping("/app")
public class AppCouponController {

    private final CouponService couponService;

    public AppCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @Operation(summary = "我的券包", description = "按登录态返回当前会员的券；status 为空返回全部。")
    @GetMapping("/coupons")
    public Result<List<CouponVO>> coupons(
            @Parameter(description = "券状态：0 未使用，1 已使用，2 已过期；不传返回全部")
            @RequestParam(required = false) Byte status) {
        StpKit.CUSTOMER.checkLogin();
        Long customerId = StpKit.currentCustomerId();
        return Result.success(couponService.listByCustomer(customerId, status));
    }
}
