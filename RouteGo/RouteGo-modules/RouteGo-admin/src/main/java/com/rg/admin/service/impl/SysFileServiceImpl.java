package com.rg.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.admin.config.StorageProperties;
import com.rg.admin.dto.FileQuery;
import com.rg.admin.entity.SysFile;
import com.rg.admin.mapper.SysFileMapper;
import com.rg.admin.service.SysFileService;
import com.rg.admin.vo.SysFileVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import com.rg.common.web.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements SysFileService {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final SysFileMapper fileMapper;
    private final StorageProperties properties;
    private final CacheHelper cacheHelper;

    public SysFileServiceImpl(SysFileMapper fileMapper, StorageProperties properties,
                              CacheHelper cacheHelper) {
        this.fileMapper = fileMapper;
        this.properties = properties;
        this.cacheHelper = cacheHelper;
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
    @Transactional
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
        String relativePath = datePath + "/" + storedName;
        Path base = Path.of(properties.getLocalPath());
        try {
            Files.createDirectories(base.resolve(datePath));
            Files.copy(file.getInputStream(), base.resolve(relativePath));
        } catch (IOException e) {
            throw new AdminException("文件保存失败");
        }

        SysFile record = new SysFile();
        record.setFileName(storedName);
        record.setOriginalName(originalName);
        record.setFilePath(relativePath);
        record.setFileType(extension);
        record.setFileSize(file.getSize());
        record.setStorageType((byte) 1);
        fileMapper.insert(record);

        record.setFileUrl("/admin/system/file/" + record.getId() + "/download");
        fileMapper.updateById(record);
        return toVO(record);
    }

    @Override
    public Resource download(Long id) {
        SysFile record = requireFile(id);
        Path path = Path.of(properties.getLocalPath()).resolve(record.getFilePath());
        if (!Files.exists(path)) {
            throw new AdminException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        return new FileSystemResource(path);
    }

    @Override
    @Transactional
    public void deleteFile(Long id) {
        SysFile record = requireFile(id);
        fileMapper.deleteById(id);
        try {
            Files.deleteIfExists(Path.of(properties.getLocalPath()).resolve(record.getFilePath()));
        } catch (IOException e) {
            log.warn("物理文件删除失败: {}", record.getFilePath());
        }
        cacheHelper.evictAfterCommit(String.format(CacheKeyConstants.Admin.FILE_BY_ID, id));
    }

    private SysFile requireFile(Long id) {
        SysFile file = cacheHelper.getOrLoad(String.format(CacheKeyConstants.Admin.FILE_BY_ID, id),
                TTL, () -> fileMapper.selectById(id));
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
        vo.setFileType(file.getFileType());
        vo.setFileSize(file.getFileSize());
        vo.setStorageType(file.getStorageType());
        vo.setCreateTime(file.getCreateTime());
        return vo;
    }
}
