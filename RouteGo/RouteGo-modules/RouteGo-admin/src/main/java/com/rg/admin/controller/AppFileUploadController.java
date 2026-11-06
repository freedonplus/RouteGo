package com.rg.admin.controller;

import com.rg.admin.service.SysFileService;
import com.rg.admin.vo.SysFileVO;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.security.StpKit;
import com.rg.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

/**
 * C 端图片上传。
 * <p>
 * 客服对话里客户要能发照片，但后台的上传接口带 {@code admin:SysFile:upload} 权限码，
 * 会员账号调不通。这里开一个只面向会员的窄口子：
 * <ul>
 *   <li>只允许图片（jpg/jpeg/png/webp），不放开 pdf 等可执行附件；</li>
 *   <li>单张上限 5MB，比后台的 20MB 更严；</li>
 *   <li>必须会员登录，未登录直接 401。</li>
 * </ul>
 * 返回的 {@code previewUrl} 是带签名临时链接，可直接塞进 {@code <image>}；
 * 提交消息时后端会把它还原成标准下载地址落库，读取时重新签发，避免链接过期。
 */
@Tag(name = "App-图片上传", description = "客服对话中的图片上传，仅限会员且仅限图片")
@RestController
@RequestMapping("/app/file")
public class AppFileUploadController {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final SysFileService fileService;

    public AppFileUploadController(SysFileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "上传图片", description = "需会员登录；jpg/png/webp，单张不超过 5MB")
    @PostMapping("/upload")
    public Result<SysFileVO> upload(@RequestPart("file") MultipartFile file) {
        StpKit.CUSTOMER.checkLogin();
        if (file == null || file.isEmpty()) {
            throw new AdminException(ErrorCode.BAD_REQUEST, "请选择要上传的图片");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!IMAGE_EXTENSIONS.contains(extension)) {
            throw new AdminException(ErrorCode.BAD_REQUEST, "只支持 jpg、png、webp 格式的图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new AdminException(ErrorCode.BAD_REQUEST, "图片不能超过 5MB");
        }
        return Result.success(fileService.upload(file));
    }

    private String extensionOf(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
