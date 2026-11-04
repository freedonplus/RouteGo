package com.rg.admin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rg.admin.dto.MenuUpdateRequest;
import com.rg.admin.entity.SysMenu;
import com.rg.admin.mapper.AdminRelationMapper;
import com.rg.admin.mapper.SysMenuMapper;
import com.rg.admin.mapper.SysRoleMapper;
import com.rg.admin.mapper.SysRoleMenuMapper;
import com.rg.admin.service.impl.SysMenuServiceImpl;
import com.rg.admin.vo.MenuNodeVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.exception.serviceException.AdminException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysMenuServiceImplTest {

    @Mock
    private SysMenuMapper menuMapper;
    @Mock
    private SysRoleMenuMapper roleMenuMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private AdminRelationMapper relationMapper;
    @Mock
    private CacheHelper cacheHelper;

    private SysMenuServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysMenu.class);
    }

    @BeforeEach
    void setUp() {
        service = new SysMenuServiceImpl(menuMapper, roleMenuMapper, roleMapper,
                relationMapper, cacheHelper);
        org.mockito.Mockito.lenient()
                .when(cacheHelper.getOrLoad(any(), any(), any()))
                .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(2)).get());
    }

    @Test
    void buildsTreeSortedBySortThenId() {
        when(menuMapper.selectList(any())).thenReturn(List.of(
                menu(1L, 0L, 0),
                menu(3L, 1L, 2),
                menu(2L, 1L, 1)));

        List<MenuNodeVO> tree = service.tree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getChildren())
                .extracting(MenuNodeVO::getId)
                .containsExactly(2L, 3L);
    }

    @Test
    void rejectsParentCycle() {
        when(menuMapper.selectById(1L)).thenReturn(menu(1L, 0L, 0));
        when(menuMapper.selectById(2L)).thenReturn(menu(2L, 1L, 0));

        MenuUpdateRequest request = new MenuUpdateRequest();
        request.setVersion(0);
        request.setMenuName("菜单");
        request.setMenuType("C");
        request.setParentId(2L);

        assertThatThrownBy(() -> service.updateMenu(1L, request))
                .isInstanceOf(AdminException.class)
                .hasMessageContaining("父级");
    }

    @Test
    void deleteBlockedWhenChildrenExist() {
        when(menuMapper.selectById(1L)).thenReturn(menu(1L, 0L, 0));
        when(menuMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteMenu(1L))
                .isInstanceOf(AdminException.class)
                .hasMessageContaining("子菜单");
    }

    @Test
    void superAdminGetsAllEnabledMenus() {
        when(roleMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("super_admin"));
        when(menuMapper.selectList(any())).thenReturn(List.of(
                menu(1L, 0L, 0),
                menu(2L, 1L, 0)));

        assertThat(service.userTree(1L)).isNotEmpty();
    }

    private SysMenu menu(Long id, Long parentId, Integer sort) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setMenuName("菜单" + id);
        menu.setMenuType("C");
        menu.setSort(sort);
        menu.setVisible((byte) 1);
        menu.setStatus((byte) 1);
        menu.setVersion(0);
        return menu;
    }
}
