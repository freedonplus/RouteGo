package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.admin.dto.LoginLogQuery;
import com.rg.admin.service.SysLoginLogService;
import com.rg.admin.vo.SysLoginLogVO;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "登录日志", description = "登录日志只读查询")
@RestController
@RequestMapping("/admin/system/login-log")
public class SysLoginLogController {

    private final SysLoginLogService loginLogService;

    public SysLoginLogController(SysLoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @Operation(summary = "登录日志分页")
    @SaCheckPermission("admin:SysLoginLog:page")
    @GetMapping("/page")
    public Result<PageResult<SysLoginLogVO>> page(@Valid LoginLogQuery query) {
        return Result.success(loginLogService.page(query));
    }
}
