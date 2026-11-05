package com.rg.admin.storage;

import java.io.InputStream;

/**
 * 文件存储抽象：屏蔽本地磁盘 / MinIO 等实现差异。
 * <p>
 * objectKey 统一使用「yyyy/MM/dd/uuid.ext」格式，与 sys_file.file_path 一致。
 */
public interface FileStorage {

    /** 配置类型标识：local / minio */
    String type();

    /** 落库到 sys_file.storage_type：1 本地，3 MinIO */
    byte storageType();

    /** 保存文件流；objectKey 已含日期路径与最终文件名 */
    void store(InputStream in, long size, String contentType, String objectKey);

    /** 打开对象输入流（读完后由调用方负责关闭） */
    InputStream open(String objectKey);

    /** 删除对象；不存在时静默成功 */
    void delete(String objectKey);

    /** 轻量探活，供仪表盘系统状态展示；默认视为健康 */
    default boolean healthy() {
        return true;
    }
}
