package com.rg.admin.mapper;

import com.baomidou.mybatisplus.annotation.Version;
import com.rg.admin.entity.SysMenu;
import com.rg.admin.entity.SysRole;
import com.rg.admin.entity.SysUser;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MapperContractTest {

    @Test
    void versionFieldsAreAnnotated() throws Exception {
        for (Class<?> type : List.of(SysUser.class, SysRole.class, SysMenu.class)) {
            assertThat(type.getDeclaredField("version").isAnnotationPresent(Version.class))
                    .as(type.getSimpleName() + ".version")
                    .isTrue();
        }
    }

    @Test
    void permissionSqlFiltersRoleState() throws Exception {
        String sql = SysPermissionMapper.class
                .getMethod("selectPermissionCodesByUserId", Long.class)
                .getAnnotation(Select.class).value()[0];

        assertThat(sql).contains("JOIN sys_role")
                .contains("r.status = 1")
                .contains("r.deleted = 0");
    }
}
