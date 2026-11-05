package com.rg.admin.storage;

import com.rg.admin.config.StorageProperties;
import com.rg.common.exception.serviceException.AdminException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

/**
 * MinIO 对象存储实现（routego.storage.type=minio 时启用）。
 * <p>
 * MinioClient 构造不发生网络请求，应用启动不依赖 MinIO 存活；
 * bucket 采用首次上传时懒创建，MinIO 暂不可用只影响上传/下载动作本身。
 */
@Slf4j
public class MinioFileStorage implements FileStorage {

    private final StorageProperties properties;
    private final MinioClient client;
    private volatile boolean bucketReady;

    public MinioFileStorage(StorageProperties properties) {
        this.properties = properties;
        StorageProperties.Minio minio = properties.getMinio();
        this.client = MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }

    @Override
    public String type() {
        return "minio";
    }

    @Override
    public byte storageType() {
        return (byte) 3;
    }

    @Override
    public void store(InputStream in, long size, String contentType, String objectKey) {
        ensureBucket();
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getMinio().getBucket())
                    .object(objectKey)
                    .stream(in, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new AdminException("文件上传到 MinIO 失败", e);
        }
    }

    @Override
    public InputStream open(String objectKey) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(properties.getMinio().getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new AdminException("文件从 MinIO 读取失败", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getMinio().getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            // 删除失败不抛出，仅告警：DB 记录已删，孤儿对象由 MinIO 生命周期策略兜底清理
            log.warn("MinIO 对象删除失败: {}", objectKey, e);
        }
    }

    @Override
    public boolean healthy() {
        try {
            return client.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.getMinio().getBucket())
                    .build());
        } catch (Exception e) {
            return false;
        }
    }

    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            try {
                String bucket = properties.getMinio().getBucket();
                if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("MinIO bucket 已创建: {}", bucket);
                }
                bucketReady = true;
            } catch (Exception e) {
                throw new AdminException("MinIO 初始化失败，请检查存储服务是否可用", e);
            }
        }
    }
}
