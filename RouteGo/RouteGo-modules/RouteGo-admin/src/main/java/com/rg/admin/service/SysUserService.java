package com.rg.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.admin.dto.AssignRolesRequest;
import com.rg.admin.dto.ResetPasswordRequest;
import com.rg.admin.dto.UserCreateRequest;
import com.rg.admin.dto.UserQuery;
import com.rg.admin.dto.UserUpdateRequest;
import com.rg.admin.entity.SysUser;
import com.rg.admin.vo.SysUserDetailVO;
import com.rg.admin.vo.SysUserVO;
import com.rg.common.web.PageResult;

import java.util.List;

/**
 * 系统用户服务。
 */
public interface SysUserService extends IService<SysUser> {

    PageResult<SysUserVO> page(UserQuery query);

    SysUserDetailVO detail(Long id);

    Long create(UserCreateRequest request);

    void update(Long id, UserUpdateRequest request);

    void updateStatus(Long id, Byte status);

    void resetPassword(Long id, ResetPasswordRequest request);

    void assignRoles(Long id, AssignRolesRequest request);

    void deleteUser(Long id);

    List<Long> listRoleIds(Long userId);
}
