package com.rg.operations.controller.app;

import com.rg.common.security.StpKit;
import com.rg.common.util.Result;
import com.rg.operations.dto.CustomRequirementCreateRequest;
import com.rg.operations.service.CustomRequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端定制需求提交。
 * <p>
 * 会员 ID 取登录态，客户端无法伪造归属；落库后由运营在后台分配顾问并报价。
 * 业务逻辑全部在 {@link CustomRequirementService}，本类只做参数转换。
 */
@Tag(name = "App-定制", description = "提交定制需求")
@RestController
@RequestMapping("/app/custom")
public class AppCustomController {

    private final CustomRequirementService customRequirementService;

    public AppCustomController(CustomRequirementService customRequirementService) {
        this.customRequirementService = customRequirementService;
    }

    @Operation(summary = "提交定制需求", description = "需要会员登录；会员 ID 由后端从登录态注入。")
    @PostMapping
    public Result<Long> apply(@Valid @RequestBody CustomRequirementCreateRequest request) {
        StpKit.CUSTOMER.checkLogin();
        request.setCustomerId(StpKit.currentCustomerId());
        // 处理人由后台分配，客户端不允许指定
        request.setHandlerId(null);
        return Result.success(customRequirementService.create(request));
    }
}
