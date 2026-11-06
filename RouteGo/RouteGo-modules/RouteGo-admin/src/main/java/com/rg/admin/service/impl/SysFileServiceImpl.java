package com.rg.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.admin.config.StorageProperties;
import com.rg.admin.dto.FileQuery;
import com.rg.admin.entity.SysFile;
import com.rg.admin.mapper.SysFileMapper;
import com.rg.admin.model.SysFileCache;
import com.rg.admin.service.SysFileService;
import com.rg.admin.storage.FileStorage;
import com.rg.admin.vo.SysFileVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.storage.TempUrlSigner;
import com.rg.common.web.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements SysFileService {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "pdf", "application/pdf");

    private final SysFileMapper fileMapper;
    private final StorageProperties properties;
    private final CacheHelper cacheHelper;
    private final FileStorage storage;
    private final TempUrlSigner tempUrlSigner;

    public SysFileServiceImpl(SysFileMapper fileMapper, StorageProperties properties,
                              CacheHelper cacheHelper, FileStorage storage, TempUrlSigner tempUrlSigner) {
        this.fileMapper = fileMapper;
        this.properties = properties;
        this.cacheHelper = cacheHelper;
        this.storage = storage;
        this.tempUrlSigner = tempUrlSigner;
    }

    @Override
    public PageResult<SysFileVO> page(FileQuery query) {
        LambdaQueryWrapper<SysFile> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getOriginalName())) {
            wrapper.like(SysFile::getOriginalName, query.getOriginalName());
        }
        if (StringUtils.hasText(query.getFileType())) {
            wrapper.eq(SysFile::getFileType, query.getFileType());
        }
        wrapper.orderByDesc(SysFile::getId);
        Page<SysFile> page = fileMapper.selectPage(query.toPage(), wrapper);
        List<SysFileVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public SysFileVO detail(Long id) {
        return toVO(requireFile(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFileVO upload(MultipartFile file) {
        String originalName = sanitizeName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!properties.getAllowedExtensions().contains(extension)) {
            throw new AdminException("不支持的文件类型");
        }
        if (file.getSize() > properties.getMaxSize()) {
            throw new AdminException("文件大小超过限制");
        }

        String datePath = LocalDate.now().format(DATE_PATH);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String objectKey = datePath + "/" + storedName;
        try {
            storage.store(file.getInputStream(), file.getSize(), contentTypeOf(extension), objectKey);
        } catch (AdminException e) {
            throw e;
        } catch (Exception e) {
            throw new AdminException("文件保存失败", e);
        }
        // 事务回滚时清理已写入的对象，避免产生无 DB 记录的孤儿文件
        registerRollbackCleanup(objectKey);

        SysFile record = new SysFile();
        record.setFileName(storedName);
        record.setOriginalName(originalName);
        record.setFilePath(objectKey);
        record.setFileType(extension);
        record.setFileSize(file.getSize());
        record.setStorageType(storage.storageType());
        fileMapper.insert(record);

        record.setFileUrl("/admin/system/file/" + record.getId() + "/download");
        fileMapper.updateById(record);
        return toVO(record);
    }

    @Override
    public Resource download(Long id) {
        SysFileCache record = requireFile(id);
        // 读成字节数组：InputStreamResource 只能读一次，HTTP 层会多次访问流导致 500；
        // 文件上限 20MB，内存可控，且 ByteArrayResource 自带 content-length
        try (InputStream in = storage.open(record.getFilePath())) {
            byte[] bytes = in.readAllBytes();
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return record.getFileName();
                }
            };
        } catch (AdminException e) {
            throw e;
        } catch (Exception e) {
            throw new AdminException("文件读取失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long id) {
        SysFileCache record = requireFile(id);
        fileMapper.deleteById(id);
        cacheHelper.evictAfterCommit(String.format(CacheKeyConstants.Admin.FILE_BY_ID, id));
        // 对象删除必须等事务提交后再执行，提交失败时 DB 记录仍在而对象已丢
        Runnable cleanup = () -> {
            try {
                storage.delete(record.getFilePath());
            } catch (Exception e) {
                log.warn("存储对象删除失败: {}", record.getFilePath(), e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanup.run();
                }
            });
        } else {
            cleanup.run();
        }
    }

    /** 图片/PDF 直接内联展示时使用的 MIME 类型 */
    public static String contentTypeOf(String extension) {
        return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    public static boolean isPreviewable(String extension) {
        return CONTENT_TYPES.containsKey(extension);
    }

    private void registerRollbackCleanup(String objectKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        storage.delete(objectKey);
                    } catch (Exception e) {
                        log.warn("事务回滚后存储对象清理失败: {}", objectKey, e);
                    }
                }
            }
        });
    }

    private SysFileCache requireFile(Long id) {
        SysFileCache file = cacheHelper.getOrLoad(
                String.format(CacheKeyConstants.Admin.FILE_BY_ID, id),
                TTL,
                () -> {
                    SysFile entity = fileMapper.selectById(id);
                    return entity == null ? null : toCache(entity);
                });
        if (file == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        return file;
    }

    private String sanitizeName(String name) {
        if (name == null) {
            return "file";
        }
        return name.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
    }

    private String extensionOf(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private SysFileVO toVO(SysFile file) {
        SysFileVO vo = new SysFileVO();
        vo.setId(file.getId());
        vo.setFileName(file.getFileName());
        vo.setOriginalName(file.getOriginalName());
        vo.setFilePath(file.getFilePath());
        vo.setFileUrl(file.getFileUrl());
        vo.setPreviewUrl(tempUrlSigner.build(file.getId()));
        vo.setFileType(file.getFileType());
        vo.setFileSize(file.getFileSize());
        vo.setStorageType(file.getStorageType());
        vo.setCreateTime(file.getCreateTime());
        return vo;
    }

    private SysFileVO toVO(SysFileCache file) {
        SysFileVO vo = new SysFileVO();
        vo.setId(file.getId());
        vo.setFileName(file.getFileName());
        vo.setOriginalName(file.getOriginalName());
        vo.setFilePath(file.getFilePath());
        vo.setFileUrl(file.getFileUrl());
        vo.setPreviewUrl(tempUrlSigner.build(file.getId()));
        vo.setFileType(file.getFileType());
        vo.setFileSize(file.getFileSize());
        vo.setStorageType(file.getStorageType());
        vo.setCreateTime(file.getCreateTime());
        return vo;
    }

    private SysFileCache toCache(SysFile file) {
        SysFileCache cache = new SysFileCache();
        cache.setId(file.getId());
        cache.setFileName(file.getFileName());
        cache.setOriginalName(file.getOriginalName());
        cache.setFilePath(file.getFilePath());
        cache.setFileUrl(file.getFileUrl());
        cache.setFileType(file.getFileType());
        cache.setFileSize(file.getFileSize());
        cache.setStorageType(file.getStorageType());
        cache.setCreateTime(file.getCreateTime());
        return cache;
    }
}
