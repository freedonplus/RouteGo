package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "文件记录")
@Data
public class SysFileVO {
    private Long id;
    private String fileName;
    private String originalName;
    private String filePath;
    private String fileUrl;
    /** 临时签名预览链接（带过期时间，可直接用于 <img>，过期后自动失效；每次读取重新签发） */
    private String previewUrl;
    private String fileType;
    private Long fileSize;
    private Byte storageType;
    private LocalDateTime createTime;
}
