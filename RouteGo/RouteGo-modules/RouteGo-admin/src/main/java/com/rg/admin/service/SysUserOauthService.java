package com.rg.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.admin.dto.UserOauthBindRequest;
import com.rg.admin.entity.SysUserOauth;
import com.rg.admin.vo.SysUserOauthVO;

import java.util.List;

public interface SysUserOauthService extends IService<SysUserOauth> {

    List<SysUserOauthVO> listByUserId(Long userId);

    Long bind(UserOauthBindRequest request);

    void unbind(Long id);
}
