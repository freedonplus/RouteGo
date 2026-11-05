package com.rg.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线路评价视图对象。
 */
@Schema(description = "线路评价")
@Data
public class RouteReviewVO {

    private Long id;
    private Long routeId;
    @Schema(description = "线路标题；列表接口按需填充")
    private String routeTitle;
    private Long orderId;
    private Long customerId;
    private Byte rating;
    private String content;
    @Schema(description = "评价图片，JSON 数组字符串")
    private String images;
    private String reply;
    private LocalDateTime replyTime;
    private Byte status;
    private LocalDateTime createTime;
}
