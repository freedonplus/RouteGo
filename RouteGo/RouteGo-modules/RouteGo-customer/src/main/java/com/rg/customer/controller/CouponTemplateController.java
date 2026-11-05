package com.rg.customer.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import com.rg.customer.dto.CouponGrantRequest;
import com.rg.customer.dto.CouponTemplateQuery;
import com.rg.customer.dto.CouponTemplateSaveRequest;
import com.rg.customer.service.CouponTemplateService;
import com.rg.customer.vo.CouponTemplateVO;
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
 * 优惠券模板管理。
 * <p>
 * 模板定义券的类型、金额与有效期规则，发券动作也在这里发起。
 * 已发过券的模板不能删除，只能停用，保证存量券的券面信息可追溯。
 * <p>
 * 权限码规范：{@code customer:<Controller>:<method>}。
 */
@Tag(name = "优惠券模板", description = "券模板维护、启停、批量发券")
@RestController
@RequestMapping("/customer/coupon-template")
public class CouponTemplateController {

    private final CouponTemplateService templateService;

    public CouponTemplateController(CouponTemplateService templateService) {
        this.templateService = templateService;
    }

    @Operation(summary = "模板分页")
    @SaCheckPermission("customer:CouponTemplate:page")
    @GetMapping("/page")
    public Result<PageResult<CouponTemplateVO>> page(@Valid CouponTemplateQuery query) {
        return Result.success(templateService.page(query));
    }

    @Operation(summary = "启用中的模板列表", description = "供发券下拉框使用，已过有效期的模板不会返回")
    @SaCheckPermission("customer:CouponTemplate:list")
    @GetMapping("/list")
    public Result<List<CouponTemplateVO>> list() {
        return Result.success(templateService.listEnabled());
    }

    @Operation(summary = "模板详情")
    @SaCheckPermission("customer:CouponTemplate:detail")
    @GetMapping("/{id}")
    public Result<CouponTemplateVO> detail(@Parameter(description = "模板 ID") @PathVariable Long id) {
        return Result.success(templateService.detail(id));
    }

    @Operation(summary = "新增模板", description = "满减券填减免金额，折扣券填折扣率；有效期两种模式二选一")
    @SaCheckPermission("customer:CouponTemplate:create")
    @OperLog(title = "新增优惠券模板", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CouponTemplateSaveRequest request) {
        return Result.success(templateService.create(request));
    }

    @Operation(summary = "编辑模板", description = "改有效期会影响已发出但未使用的券，请谨慎操作")
    @SaCheckPermission("customer:CouponTemplate:update")
    @OperLog(title = "编辑优惠券模板", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CouponTemplateSaveRequest request) {
        templateService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "启用/停用模板", description = "已过有效期的模板不能启用")
    @SaCheckPermission("customer:CouponTemplate:updateStatus")
    @OperLog(title = "优惠券模板状态变更", type = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Parameter(description = "状态：0 停用，1 启用") @RequestParam Byte status) {
        templateService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "批量发券", description = "整批事务，任一会员不可发放则全部回滚；返回实际发放数量")
    @SaCheckPermission("customer:CouponTemplate:grant")
    @OperLog(title = "批量发放优惠券", type = BusinessType.INSERT)
    @PostMapping("/grant")
    public Result<Integer> grant(@Valid @RequestBody CouponGrantRequest request) {
        return Result.success(templateService.grant(request));
    }

    @Operation(summary = "删除模板", description = "已发放过券的模板不允许删除，只能停用")
    @SaCheckPermission("customer:CouponTemplate:delete")
    @OperLog(title = "删除优惠券模板", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.success();
    }
}
