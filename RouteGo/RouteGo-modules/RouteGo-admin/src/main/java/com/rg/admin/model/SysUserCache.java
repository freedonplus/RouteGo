package com.rg.admin.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户详情缓存对象。
 * <p>
 * 不含密码、不带 {@code @Sensitive}，保证缓存里是明文；HTTP 输出由
 * {@code SysUserVO} 负责脱敏。
 */
@Data
public class SysUserCache implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private Byte status;
    private Integer version;
    private Long createBy;
    private Long updateBy;
}
