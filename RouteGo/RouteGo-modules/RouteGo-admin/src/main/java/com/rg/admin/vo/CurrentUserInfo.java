package com.rg.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前登录用户信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserInfo {

    private Long userId;
    private String username;
    private String nickname;
    private List<String> roles;
    private List<String> permissions;
}
