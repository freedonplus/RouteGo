package com.rg.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "公告")
@Data
public class SysNoticeVO {
    private Long id;
    private String title;
    private String content;
    private Byte type;
    private Byte status;
    private Byte isTop;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
}
