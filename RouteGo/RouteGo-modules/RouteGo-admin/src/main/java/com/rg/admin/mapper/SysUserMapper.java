package com.rg.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.admin.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 系统用户 Mapper。
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 统计账号占用情况，包含逻辑删除行（用户名唯一索引不区分 deleted）。
     */
    @Select("SELECT COUNT(1) FROM sys_user WHERE username = #{username}")
    long countByUsernameIncludingDeleted(@Param("username") String username);

    /**
     * 批量查询用户角色名称，供列表展示。
     */
    @Select("""
            <script>
            SELECT ur.user_id AS userId, r.role_name AS roleName
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id
            WHERE r.deleted = 0
              AND ur.user_id IN
              <foreach collection="userIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<Map<String, Object>> selectRoleNamesByUserIds(@Param("userIds") List<Long> userIds);
}
