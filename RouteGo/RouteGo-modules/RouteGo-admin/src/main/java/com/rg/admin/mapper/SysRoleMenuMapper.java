package com.rg.admin.mapper;

import com.rg.admin.entity.SysRoleMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 角色菜单关联表 Mapper 接口
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-03
 */
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    /**
     * 批量写入角色菜单关系，替代循环单条 insert。
     * create_time 由表默认值填充。
     */
    @Insert("""
            <script>
            INSERT INTO sys_role_menu (role_id, menu_id) VALUES
            <foreach collection="relations" item="r" separator=",">(#{r.roleId}, #{r.menuId})</foreach>
            </script>
            """)
    int insertBatch(@Param("relations") List<SysRoleMenu> relations);
}
