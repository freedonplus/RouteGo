package com.rg.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.admin.dto.AssignIdsRequest;
import com.rg.admin.dto.RoleCreateRequest;
import com.rg.admin.dto.RoleQuery;
import com.rg.admin.dto.RoleUpdateRequest;
import com.rg.admin.entity.SysRole;
import com.rg.admin.vo.SysRoleDetailVO;
import com.rg.admin.vo.SysRoleVO;
import com.rg.admin.vo.SysUserVO;
import com.rg.common.web.PageQuery;
import com.rg.common.web.PageResult;

import java.util.List;

/**
 * 角色服务。
 */
public interface SysRoleService extends IService<SysRole> {

    PageResult<SysRoleVO> page(RoleQuery query);

    SysRoleDetailVO detail(Long id);

    Long create(RoleCreateRequest request);

    void update(Long id, RoleUpdateRequest request);

    void updateStatus(Long id, Byte status);

    void deleteRole(Long id);

    void assignMenus(Long id, AssignIdsRequest request);

    void assignPermissions(Long id, AssignIdsRequest request);

    List<Long> listMenuIds(Long roleId);

    List<Long> listPermissionIds(Long roleId);

    PageResult<SysUserVO> pageUsers(Long roleId, PageQuery pageQuery);
}
