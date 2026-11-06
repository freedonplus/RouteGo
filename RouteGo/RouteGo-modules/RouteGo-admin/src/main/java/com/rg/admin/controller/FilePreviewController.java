package com.rg.admin.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.rg.admin.service.SysFileService;
import com.rg.admin.service.impl.SysFileServiceImpl;
import com.rg.admin.vo.SysFileVO;
import com.rg.common.storage.TempUrlSigner;
import com.rg.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 文件临时签名预览（公开接口）。
 * <p>
 * <b>为什么公开</b>：图片通过 {@code <img>} 标签加载时带不上 Authorization 头，
 * H5/真机与后端也不同源（无 Cookie），直接引用下载地址会 401。本端点不校验登录态，
 * 改为校验 {@link TempUrlSigner} 签发的 HMAC 签名 + 过期时间，链接泄露后在 TTL 内自动失效。
 * 路径位于 {@code /app/**}（登录白名单），签名不合法或已过期返回 403。
 */
@Tag(name = "App-文件预览", description = "带签名的临时文件链接，供 <img> 等无法携带登录态的场景使用")
@RestController
@RequestMapping("/app/file")
public class FilePreviewController {

    private final SysFileService fileService;
    private final TempUrlSigner tempUrlSigner;

    public FilePreviewController(SysFileService fileService, TempUrlSigner tempUrlSigner) {
        this.fileService = fileService;
        this.tempUrlSigner = tempUrlSigner;
    }

    @Operation(summary = "临时签名预览",
            description = "URL 必须由后端签发（fid + expires + HMAC 签名），有效期 routego.storage.temp-url.ttl（默认 2 小时）")
    @GetMapping("/preview")
    public ResponseEntity<Resource> preview(
            @Parameter(description = "文件 ID") @RequestParam long fid,
            @Parameter(description = "过期时间（epoch 秒）") @RequestParam long expires,
            @Parameter(description = "HMAC-SHA256 签名") @RequestParam String sign) {
        if (!tempUrlSigner.verify(fid, expires, sign)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        SysFileVO file = fileService.detail(fid);
        Resource resource = fileService.download(fid);
        // 图片等可预览类型按真实 MIME 内联返回；其余按附件下载
        boolean previewable = SysFileServiceImpl.isPreviewable(file.getFileType());
        MediaType mediaType = previewable
                ? MediaType.parseMediaType(SysFileServiceImpl.contentTypeOf(file.getFileType()))
                : MediaType.APPLICATION_OCTET_STREAM;
        ContentDisposition disposition = (previewable
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(file.getOriginalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                // 同一条临时链接在 TTL 内允许浏览器缓存，减少回源
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=600")
                .body(resource);
    }

    /** 供前端调试/联调：按文件 ID 签发一条临时链接。/app/** 在登录白名单内，必须显式校验后台登录与权限 */
    @Operation(summary = "签发临时链接", description = "返回可直接用于 <img> 的带签名地址；需要后台登录且拥有文件下载权限")
    @GetMapping("/issue")
    public Result<String> issue(@Parameter(description = "文件 ID") @RequestParam long fid) {
        StpUtil.checkPermission("admin:SysFile:download");
        return Result.success(tempUrlSigner.build(fid));
    }
}
