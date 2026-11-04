package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "字典数据")
@Data
public class DictDataVO {
    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer sort;
    private String cssClass;
    private Byte isDefault;
    private Byte status;
    private String remark;
}
