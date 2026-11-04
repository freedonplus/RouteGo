package com.rg.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地文件存储配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "routego.storage")
public class StorageProperties {

    private String localPath = "./uploads";
    private long maxSize = 20 * 1024 * 1024L;
    private List<String> allowedExtensions = new ArrayList<>(
            List.of("jpg", "jpeg", "png", "webp", "pdf"));
}
