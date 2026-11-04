package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "系统参数")
@Data
public class SysConfigVO {
    private Long id;
    private String configName;
    private String configKey;
    private String configValue;
    private String remark;
    private LocalDateTime createTime;
}
