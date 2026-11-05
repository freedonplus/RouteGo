package com.rg.admin.storage;

import com.rg.admin.config.StorageProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘存储实现（routego.storage.type=local 时启用）。
 */
public class LocalFileStorage implements FileStorage {

    private final StorageProperties properties;

    public LocalFileStorage(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public String type() {
        return "local";
    }

    @Override
    public byte storageType() {
        return (byte) 1;
    }

    @Override
    public void store(InputStream in, long size, String contentType, String objectKey) {
        Path target = Path.of(properties.getLocalPath()).resolve(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("文件保存失败", e);
        }
    }

    @Override
    public InputStream open(String objectKey) {
        Path path = Path.of(properties.getLocalPath()).resolve(objectKey);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("文件不存在: " + objectKey);
        }
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new IllegalStateException("文件读取失败", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(Path.of(properties.getLocalPath()).resolve(objectKey));
        } catch (IOException e) {
            throw new IllegalStateException("文件删除失败", e);
        }
    }
}
