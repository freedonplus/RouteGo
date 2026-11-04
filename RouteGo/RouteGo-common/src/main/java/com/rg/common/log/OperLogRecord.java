package com.rg.common.log;

import com.rg.common.annotation.BusinessType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志切面采集到的原始记录，由业务模块落库。
 */
@Data
public class OperLogRecord {

    private String title;
    private BusinessType businessType;
    private String method;
    private String requestMethod;
    private Long operId;
    private String operName;
    private String operUrl;
    private String operIp;
    private String operParam;
    private String jsonResult;
    private Integer status;
    private String errorMsg;
    private Long costTime;
    private LocalDateTime operTime;
}
