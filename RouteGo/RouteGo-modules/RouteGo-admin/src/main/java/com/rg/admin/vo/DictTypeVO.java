package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "字典类型")
@Data
public class DictTypeVO {
    private Long id;
    private String dictName;
    private String dictType;
    private Byte status;
    private String remark;
    private LocalDateTime createTime;
}
