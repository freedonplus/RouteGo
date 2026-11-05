package com.rg.common.constant;

import com.rg.common.cache.CacheNullValue;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CacheKeyConstantsTest {

    private static final Map<String, String> EXPECTED_ADMIN_KEYS = new HashMap<>();

    static {
        EXPECTED_ADMIN_KEYS.put("CAPTCHA", "admin:Auth:captcha:%s");
        EXPECTED_ADMIN_KEYS.put("LOGIN_FAIL", "admin:Auth:loginFail:%s");
        EXPECTED_ADMIN_KEYS.put("LOGIN_LOCK", "admin:Auth:loginLock:%s");
        EXPECTED_ADMIN_KEYS.put("USER_BY_ID", "admin:SysUser:getById:%s");
        EXPECTED_ADMIN_KEYS.put("USER_PERMISSIONS", "admin:SysUser:getPermissions:%s");
        EXPECTED_ADMIN_KEYS.put("USER_ROLES", "admin:SysUser:getRoles:%s");
        EXPECTED_ADMIN_KEYS.put("USER_MENU_TREE", "admin:SysMenu:getUserTree:%s");
        EXPECTED_ADMIN_KEYS.put("ROLE_BY_ID", "admin:SysRole:getById:%s");
        EXPECTED_ADMIN_KEYS.put("ROLE_MENUS", "admin:SysRole:getMenus:%s");
        EXPECTED_ADMIN_KEYS.put("ROLE_PERMISSIONS", "admin:SysRole:getPermissions:%s");
        EXPECTED_ADMIN_KEYS.put("MENU_BY_ID", "admin:SysMenu:getById:%s");
        EXPECTED_ADMIN_KEYS.put("MENU_TREE", "admin:SysMenu:tree");
        EXPECTED_ADMIN_KEYS.put("PERMISSION_BY_ID", "admin:SysPermission:getById:%s");
        EXPECTED_ADMIN_KEYS.put("PERMISSION_ALL", "admin:SysPermission:listAll");
        EXPECTED_ADMIN_KEYS.put("DICT_TYPE_BY_TYPE", "admin:SysDictType:getByType:%s");
        EXPECTED_ADMIN_KEYS.put("DICT_DATA_BY_TYPE", "admin:SysDictData:listByType:%s");
        EXPECTED_ADMIN_KEYS.put("CONFIG_BY_KEY", "admin:SysConfig:getByKey:%s");
        EXPECTED_ADMIN_KEYS.put("NOTICE_PUBLISHED", "admin:SysNotice:listPublished");
        EXPECTED_ADMIN_KEYS.put("OAUTH_BY_USER", "admin:SysUserOauth:listByUserId:%s");
        EXPECTED_ADMIN_KEYS.put("FILE_BY_ID", "admin:SysFile:getById:%s");
        EXPECTED_ADMIN_KEYS.put("PERM_LIST", "admin:config:SaToken:getPerm:%s:%s");
        EXPECTED_ADMIN_KEYS.put("PERM_VERSION", "admin:config:SaToken:getPermVer");
        EXPECTED_ADMIN_KEYS.put("PERM_USER_VERSION", "admin:config:SaToken:getPermVer:%s");
        EXPECTED_ADMIN_KEYS.put("VERIFY_CODE", "admin:Auth:code:%s:%s");
        EXPECTED_ADMIN_KEYS.put("VERIFY_CODE_COOLDOWN", "admin:Auth:code:cd:%s:%s");
        EXPECTED_ADMIN_KEYS.put("VERIFY_CODE_ATTEMPT", "admin:Auth:code:try:%s:%s");
        EXPECTED_ADMIN_KEYS.put("VERIFY_CODE_DAILY", "admin:Auth:code:daily:%s");
        EXPECTED_ADMIN_KEYS.put("VERIFY_CODE_IP", "admin:Auth:code:ip:%s");
        EXPECTED_ADMIN_KEYS.put("OAUTH_STATE", "admin:Auth:oauth:state:%s");
        EXPECTED_ADMIN_KEYS.put("IP_LOCATION", "admin:Ip:location:%s");
    }

    @Test
    void keyTemplatesFollowModuleTableMethodFormat() {
        assertThat(CacheKeyConstants.Admin.USER_BY_ID).isEqualTo("admin:SysUser:getById:%s");
        assertThat(CacheKeyConstants.Admin.DICT_DATA_BY_TYPE).isEqualTo("admin:SysDictData:listByType:%s");
        assertThat(CacheKeyConstants.Lock.CACHE_REBUILD).isEqualTo("lock:cache:rebuild:%s");
        assertThat(String.format(CacheKeyConstants.Admin.USER_BY_ID, 7)).isEqualTo("admin:SysUser:getById:7");
    }

    @Test
    void everyAdminKeyIsDeclaredAndMatchesTheCatalog() throws Exception {
        Map<String, String> actual = new HashMap<>();
        for (Field field : CacheKeyConstants.Admin.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                actual.put(field.getName(), (String) field.get(null));
            }
        }
        assertThat(actual).isEqualTo(EXPECTED_ADMIN_KEYS);
    }

    @Test
    void nullSentinelIsSingletonSerializable() {
        assertThat(CacheNullValue.INSTANCE).isSameAs(CacheNullValue.INSTANCE);
        assertThat(CacheNullValue.INSTANCE).isInstanceOf(Serializable.class);
    }
}
