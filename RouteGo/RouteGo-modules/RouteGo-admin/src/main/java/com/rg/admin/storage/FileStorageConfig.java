package com.rg.admin.storage;

import com.rg.admin.config.StorageProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 按 routego.storage.type 装配具体存储实现，默认 MinIO。
 */
@Configuration
public class FileStorageConfig {

    @Bean
    public FileStorage fileStorage(StorageProperties properties) {
        return switch (properties.getType()) {
            case "minio" -> new MinioFileStorage(properties);
            case "local" -> new LocalFileStorage(properties);
            default -> throw new IllegalStateException("未知存储类型: " + properties.getType());
        };
    }
}
