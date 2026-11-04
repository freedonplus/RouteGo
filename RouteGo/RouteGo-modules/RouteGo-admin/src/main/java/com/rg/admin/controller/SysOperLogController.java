package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.admin.dto.OperLogQuery;
import com.rg.admin.service.SysOperLogService;
import com.rg.admin.vo.SysOperLogVO;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "操作日志", description = "操作日志只读查询")
@RestController
@RequestMapping("/admin/system/oper-log")
public class SysOperLogController {

    private final SysOperLogService operLogService;

    public SysOperLogController(SysOperLogService operLogService) {
        this.operLogService = operLogService;
    }

    @Operation(summary = "操作日志分页")
    @SaCheckPermission("admin:SysOperLog:page")
    @GetMapping("/page")
    public Result<PageResult<SysOperLogVO>> page(@Valid OperLogQuery query) {
        return Result.success(operLogService.page(query));
    }

    @Operation(summary = "操作日志详情")
    @SaCheckPermission("admin:SysOperLog:detail")
    @GetMapping("/{id}")
    public Result<SysOperLogVO> detail(@Parameter(description = "主键 ID") @PathVariable Long id) {
        return Result.success(operLogService.detail(id));
    }
}
