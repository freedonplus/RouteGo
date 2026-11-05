package com.rg.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 线路分类视图对象，树形结构。
 */
@Schema(description = "线路分类（树节点）")
@Data
public class RouteCategoryVO {

    private Long id;
    private Long parentId;
    private String name;
    private Integer sort;
    private Byte status;
    private String icon;
    private LocalDateTime createTime;

    @Schema(description = "子分类")
    private List<RouteCategoryVO> children = new ArrayList<>();
}
