package com.rg.admin.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色详情缓存对象。
 * <p>
 * 与实体解耦：不携带逻辑删除等内部字段，避免缓存反序列化出的对象
 * 被误用于 {@code updateById} 时携带过期状态。
 */
@Data
public class SysRoleCache implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String roleName;
    private String roleCode;
    private Integer sort;
    private Byte status;
    private String remark;
    private Integer version;
    private LocalDateTime createTime;
}
