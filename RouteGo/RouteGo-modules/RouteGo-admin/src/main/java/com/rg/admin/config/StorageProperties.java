package com.rg.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件存储配置。type=minio 走 MinIO 对象存储，type=local 走本地磁盘兜底。
 */
@Data
@Component
@ConfigurationProperties(prefix = "routego.storage")
public class StorageProperties {

    /** 存储实现：minio（默认）/ local */
    private String type = "minio";
    private String localPath = "./uploads";
    private long maxSize = 20 * 1024 * 1024L;
    private List<String> allowedExtensions = new ArrayList<>(
            List.of("jpg", "jpeg", "png", "webp", "pdf"));
    private final Minio minio = new Minio();

    @Data
    public static class Minio {
        private String endpoint = "http://127.0.0.1:9000";
        private String accessKey = "minio";
        private String secretKey = "";
        private String bucket = "routego";
    }
}
