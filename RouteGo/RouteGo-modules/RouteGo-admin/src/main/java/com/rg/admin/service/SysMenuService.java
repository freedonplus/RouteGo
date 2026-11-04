package com.rg.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rg.admin.dto.MenuCreateRequest;
import com.rg.admin.dto.MenuUpdateRequest;
import com.rg.admin.entity.SysMenu;
import com.rg.admin.vo.MenuNodeVO;
import com.rg.admin.vo.SysMenuVO;

import java.util.List;

/**
 * 菜单服务。
 */
public interface SysMenuService extends IService<SysMenu> {

    List<MenuNodeVO> tree();

    List<MenuNodeVO> userTree(Long userId);

    SysMenuVO detail(Long id);

    Long createMenu(MenuCreateRequest request);

    void updateMenu(Long id, MenuUpdateRequest request);

    void deleteMenu(Long id);

    void updateStatus(Long id, Byte status);

    void updateVisible(Long id, Byte visible);
}
