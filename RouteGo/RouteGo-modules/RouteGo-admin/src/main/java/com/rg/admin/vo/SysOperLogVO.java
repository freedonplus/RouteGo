package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "操作日志")
@Data
public class SysOperLogVO {
    private Long id;
    private String title;
    private Byte businessType;
    private String method;
    private String requestMethod;
    private String operName;
    private String operUrl;
    private String operIp;
    private String operParam;
    private String jsonResult;
    private Byte status;
    private String errorMsg;
    private Long costTime;
    private LocalDateTime operTime;
}
