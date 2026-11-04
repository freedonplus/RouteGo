package com.rg.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.admin.dto.PermissionCreateRequest;
import com.rg.admin.dto.PermissionQuery;
import com.rg.admin.dto.PermissionUpdateRequest;
import com.rg.admin.entity.SysPermission;
import com.rg.admin.vo.PermissionGroupVO;
import com.rg.admin.vo.SysPermissionVO;
import com.rg.common.web.PageResult;

import java.util.List;

/**
 * 权限点服务。
 */
public interface SysPermissionService extends IService<SysPermission> {

    PageResult<SysPermissionVO> page(PermissionQuery query);

    List<PermissionGroupVO> grouped();

    SysPermissionVO detail(Long id);

    Long createPermission(PermissionCreateRequest request);

    void updatePermission(Long id, PermissionUpdateRequest request);

    void deletePermission(Long id);

    void updateStatus(Long id, Byte status);
}
