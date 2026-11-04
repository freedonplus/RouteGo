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
    private String fileType;
    private Long fileSize;
    private Byte storageType;
    private LocalDateTime createTime;
}
