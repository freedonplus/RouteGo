package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.admin.service.DashboardService;
import com.rg.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 仪表盘接口；聚合逻辑见 {@link DashboardService}。
 */
@Tag(name = "仪表盘")
@RestController
@RequestMapping("/admin/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "仪表盘概览",
            description = "仅返回聚合统计与脱敏摘要；需要 admin:Dashboard:overview 权限。")
    @SaCheckPermission("admin:Dashboard:overview")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(dashboardService.overview());
    }
}
