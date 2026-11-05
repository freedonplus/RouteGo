package com.rg.customer.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import com.rg.customer.dto.CustomerCreateRequest;
import com.rg.customer.dto.CustomerPasswordRequest;
import com.rg.customer.dto.CustomerPhoneRequest;
import com.rg.customer.dto.CustomerQuery;
import com.rg.customer.dto.CustomerUpdateRequest;
import com.rg.customer.service.CouponService;
import com.rg.customer.service.CustomerService;
import com.rg.customer.vo.CouponVO;
import com.rg.customer.vo.CustomerVO;
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
 * 会员管理。
 * <p>
 * 手机号、证件号在接口出口一律脱敏；换绑手机号、重置密码、调整积分单独开接口，
 * 便于在操作日志里把敏感变更与普通资料编辑区分开。
 * <p>
 * 权限码规范：{@code customer:<Controller>:<method>}。
 */
@Tag(name = "会员管理", description = "会员查询、代注册、资料维护、手机号换绑、积分与等级调整")
@RestController
@RequestMapping("/customer/customer")
public class CustomerController {

    private final CustomerService customerService;
    private final CouponService couponService;

    public CustomerController(CustomerService customerService, CouponService couponService) {
        this.customerService = customerService;
        this.couponService = couponService;
    }

    @Operation(summary = "会员分页", description = "手机号按盲索引精确匹配；关键词模糊匹配昵称与真实姓名")
    @SaCheckPermission("customer:Customer:page")
    @GetMapping("/page")
    public Result<PageResult<CustomerVO>> page(@Valid CustomerQuery query) {
        return Result.success(customerService.page(query));
    }

    @Operation(summary = "会员详情", description = "附带可用券与已用券数量")
    @SaCheckPermission("customer:Customer:detail")
    @GetMapping("/{id}")
    public Result<CustomerVO> detail(@Parameter(description = "会员 ID") @PathVariable Long id) {
        return Result.success(customerService.detail(id));
    }

    @Operation(summary = "新增会员", description = "后台代注册；手机号唯一，密码可留空（仅验证码登录）")
    @SaCheckPermission("customer:Customer:create")
    @OperLog(title = "新增会员", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CustomerCreateRequest request) {
        return Result.success(customerService.create(request));
    }

    @Operation(summary = "编辑会员资料", description = "不含手机号、密码与积分，这三项分别走独立接口")
    @SaCheckPermission("customer:Customer:update")
    @OperLog(title = "编辑会员", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest request) {
        customerService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "手机号换绑", description = "同时更新密文列与盲索引列，新号码不能与他人重复")
    @SaCheckPermission("customer:Customer:updatePhone")
    @OperLog(title = "会员手机号换绑", type = BusinessType.UPDATE)
    @PutMapping("/{id}/phone")
    public Result<Void> updatePhone(@PathVariable Long id, @Valid @RequestBody CustomerPhoneRequest request) {
        customerService.updatePhone(id, request);
        return Result.success();
    }

    @Operation(summary = "重置登录密码")
    @SaCheckPermission("customer:Customer:resetPassword")
    @OperLog(title = "重置会员密码", type = BusinessType.UPDATE)
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody CustomerPasswordRequest request) {
        customerService.resetPassword(id, request);
        return Result.success();
    }

    @Operation(summary = "启用/禁用会员", description = "禁用后会员无法登录，已领取的券不受影响")
    @SaCheckPermission("customer:Customer:updateStatus")
    @OperLog(title = "会员状态变更", type = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Parameter(description = "状态：0 禁用，1 正常") @RequestParam Byte status) {
        customerService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "调整会员等级", description = "1 普通，2 银卡，3 金卡，4 钻石")
    @SaCheckPermission("customer:Customer:updateLevel")
    @OperLog(title = "会员等级调整", type = BusinessType.UPDATE)
    @PutMapping("/{id}/level")
    public Result<Void> updateLevel(@PathVariable Long id,
                                    @Parameter(description = "会员等级 1~4") @RequestParam Byte level) {
        customerService.updateLevel(id, level);
        return Result.success();
    }

    @Operation(summary = "调整积分", description = "传正数加分、负数扣分，结果不会小于 0")
    @SaCheckPermission("customer:Customer:adjustPoints")
    @OperLog(title = "会员积分调整", type = BusinessType.UPDATE)
    @PostMapping("/{id}/points")
    public Result<Void> adjustPoints(@PathVariable Long id,
                                     @Parameter(description = "积分变化量，可为负") @RequestParam Integer delta) {
        customerService.adjustPoints(id, delta);
        return Result.success();
    }

    @Operation(summary = "会员券包", description = "该会员名下的优惠券，可按状态过滤")
    @SaCheckPermission("customer:Customer:listCoupons")
    @GetMapping("/{id}/coupons")
    public Result<List<CouponVO>> listCoupons(@PathVariable Long id,
                                              @Parameter(description = "0 未使用，1 已使用，2 已过期")
                                              @RequestParam(required = false) Byte status) {
        return Result.success(couponService.listByCustomer(id, status));
    }

    @Operation(summary = "删除会员", description = "逻辑删除，未使用的券同步作废；订单记录保留")
    @SaCheckPermission("customer:Customer:delete")
    @OperLog(title = "删除会员", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.success();
    }
}
