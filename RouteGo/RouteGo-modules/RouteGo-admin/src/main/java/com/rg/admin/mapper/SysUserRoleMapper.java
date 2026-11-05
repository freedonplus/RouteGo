package com.rg.admin.mapper;

import com.rg.admin.entity.SysUserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户角色关联表 Mapper 接口
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-03
 */
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 批量写入用户角色关系，替代循环单条 insert。
     * create_time 由表默认值填充。
     */
    @Insert("""
            <script>
            INSERT INTO sys_user_role (user_id, role_id) VALUES
            <foreach collection="relations" item="r" separator=",">(#{r.userId}, #{r.roleId})</foreach>
            </script>
            """)
    int insertBatch(@Param("relations") List<SysUserRole> relations);
}
