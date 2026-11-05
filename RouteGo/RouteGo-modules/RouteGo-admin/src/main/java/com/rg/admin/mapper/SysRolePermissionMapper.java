package com.rg.admin.mapper;

import com.rg.admin.entity.SysRolePermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 角色权限关联表 Mapper 接口
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-03
 */
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 批量写入角色权限关系，替代循环单条 insert。
     * create_time 由表默认值填充。
     */
    @Insert("""
            <script>
            INSERT INTO sys_role_permission (role_id, permission_id) VALUES
            <foreach collection="relations" item="r" separator=",">(#{r.roleId}, #{r.permissionId})</foreach>
            </script>
            """)
    int insertBatch(@Param("relations") List<SysRolePermission> relations);
}
