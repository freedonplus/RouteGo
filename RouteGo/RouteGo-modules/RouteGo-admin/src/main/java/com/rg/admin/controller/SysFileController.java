package com.rg.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rg.admin.dto.FileQuery;
import com.rg.admin.service.SysFileService;
import com.rg.admin.vo.SysFileVO;
import com.rg.common.annotation.BusinessType;
import com.rg.common.annotation.OperLog;
import com.rg.common.util.Result;
import com.rg.common.web.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Tag(name = "文件管理", description = "文件上传、分页、下载与删除")
@RestController
@RequestMapping("/admin/system/file")
public class SysFileController {

    private final SysFileService fileService;

    public SysFileController(SysFileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "上传文件")
    @SaCheckPermission("admin:SysFile:upload")
    @OperLog(title = "文件管理", type = BusinessType.INSERT)
    @PostMapping("/upload")
    public Result<SysFileVO> upload(@RequestPart("file") MultipartFile file) {
        return Result.success(fileService.upload(file));
    }

    @Operation(summary = "文件分页")
    @SaCheckPermission("admin:SysFile:page")
    @GetMapping("/page")
    public Result<PageResult<SysFileVO>> page(@Valid FileQuery query) {
        return Result.success(fileService.page(query));
    }

    @Operation(summary = "文件详情")
    @SaCheckPermission("admin:SysFile:detail")
    @GetMapping("/{id}")
    public Result<SysFileVO> detail(@Parameter(description = "主键 ID") @PathVariable Long id) {
        return Result.success(fileService.detail(id));
    }

    @Operation(summary = "下载文件")
    @SaCheckPermission("admin:SysFile:download")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@Parameter(description = "主键 ID") @PathVariable Long id) {
        SysFileVO file = fileService.detail(id);
        Resource resource = fileService.download(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.getOriginalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    @Operation(summary = "删除文件")
    @SaCheckPermission("admin:SysFile:delete")
    @OperLog(title = "文件管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "主键 ID") @PathVariable Long id) {
        fileService.deleteFile(id);
        return Result.success();
    }
}
