package com.rg.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rg.admin.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统用户 Mapper。
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 统计账号占用情况，包含逻辑删除行（用户名唯一索引不区分 deleted）。
     */
    @Select("SELECT COUNT(1) FROM sys_user WHERE username = #{username}")
    long countByUsernameIncludingDeleted(@Param("username") String username);
}
