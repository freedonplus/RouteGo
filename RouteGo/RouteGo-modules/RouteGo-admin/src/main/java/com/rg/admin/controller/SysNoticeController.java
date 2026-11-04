package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.admin.dto.NoticeCreateRequest;
import com.rg.admin.dto.NoticeQuery;
import com.rg.admin.dto.NoticeUpdateRequest;
import com.rg.admin.service.SysNoticeService;
import com.rg.admin.vo.SysNoticeVO;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "公告管理", description = "公告 CRUD、发布/下架与置顶")
@RestController
@RequestMapping("/admin/system/notice")
public class SysNoticeController {

    private final SysNoticeService noticeService;

    public SysNoticeController(SysNoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Operation(summary = "公告分页")
    @SaCheckPermission("system:notice:list")
    @GetMapping("/page")
    public Result<PageResult<SysNoticeVO>> page(@Valid NoticeQuery query) {
        return Result.success(noticeService.page(query));
    }

    @Operation(summary = "已发布公告")
    @SaCheckLogin
    @GetMapping("/published")
    public Result<List<SysNoticeVO>> published() {
        return Result.success(noticeService.listPublished());
    }

    @Operation(summary = "公告详情")
    @SaCheckPermission("system:notice:list")
    @GetMapping("/{id}")
    public Result<SysNoticeVO> detail(@PathVariable Long id) {
        return Result.success(noticeService.detail(id));
    }

    @Operation(summary = "新增公告")
    @SaCheckPermission("system:notice:add")
    @OperLog(title = "公告管理", type = BusinessType.INSERT)
    @PostMapping
    public Result<Long> create(@Valid @RequestBody NoticeCreateRequest request) {
        return Result.success(noticeService.create(request));
    }

    @Operation(summary = "编辑公告")
    @SaCheckPermission("system:notice:edit")
    @OperLog(title = "公告管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NoticeUpdateRequest request) {
        noticeService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除公告")
    @SaCheckPermission("system:notice:delete")
    @OperLog(title = "公告管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return Result.success();
    }

    @Operation(summary = "发布公告")
    @SaCheckPermission("system:notice:edit")
    @OperLog(title = "公告管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        noticeService.publish(id);
        return Result.success();
    }

    @Operation(summary = "下架公告")
    @SaCheckPermission("system:notice:edit")
    @OperLog(title = "公告管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/unpublish")
    public Result<Void> unpublish(@PathVariable Long id) {
        noticeService.unpublish(id);
        return Result.success();
    }

    @Operation(summary = "置顶/取消置顶")
    @SaCheckPermission("system:notice:edit")
    @OperLog(title = "公告管理", type = BusinessType.UPDATE)
    @PutMapping("/{id}/top")
    public Result<Void> setTop(@PathVariable Long id, @RequestParam boolean top) {
        noticeService.setTop(id, top);
        return Result.success();
    }
}
