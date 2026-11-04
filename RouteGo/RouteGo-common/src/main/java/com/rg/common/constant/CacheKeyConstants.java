package com.rg.common.constant;

/**
 * Redis key 集中定义。
 * <p>
 * 命名规则：{@code [模块]:[表/控制器]:[方法]:[参数]}。参数位置使用
 * {@code %s} 占位，通过 {@link String#format} 生成最终 key。
 * <p>
 * 所有业务缓存 key 必须在这里声明，禁止在 Service 中散落字符串。
 */
public final class CacheKeyConstants {

    private CacheKeyConstants() {
    }

    /**
     * 后台管理模块。
     */
    public static final class Admin {

        private Admin() {
        }

        /** 图形验证码答案。 */
        public static final String CAPTCHA = "admin:Auth:captcha:%s";
        /** 登录失败计数。 */
        public static final String LOGIN_FAIL = "admin:Auth:loginFail:%s";
        /** 登录锁定标记。 */
        public static final String LOGIN_LOCK = "admin:Auth:loginLock:%s";

        /** 用户详情缓存。 */
        public static final String USER_BY_ID = "admin:SysUser:getById:%s";
        /** 用户权限码缓存。 */
        public static final String USER_PERMISSIONS = "admin:SysUser:getPermissions:%s";
        /** 用户角色码缓存。 */
        public static final String USER_ROLES = "admin:SysUser:getRoles:%s";
        /** 用户可见菜单树。 */
        public static final String USER_MENU_TREE = "admin:SysMenu:getUserTree:%s";

        /** 角色详情缓存。 */
        public static final String ROLE_BY_ID = "admin:SysRole:getById:%s";
        /** 角色菜单 ID 列表。 */
        public static final String ROLE_MENUS = "admin:SysRole:getMenus:%s";
        /** 角色权限 ID 列表。 */
        public static final String ROLE_PERMISSIONS = "admin:SysRole:getPermissions:%s";

        /** 菜单详情缓存。 */
        public static final String MENU_BY_ID = "admin:SysMenu:getById:%s";
        /** 全量菜单树。 */
        public static final String MENU_TREE = "admin:SysMenu:tree";

        /** 权限点详情缓存。 */
        public static final String PERMISSION_BY_ID = "admin:SysPermission:getById:%s";
        /** 全部权限点（分组查询）。 */
        public static final String PERMISSION_ALL = "admin:SysPermission:listAll";

        /** 字典类型缓存。 */
        public static final String DICT_TYPE_BY_TYPE = "admin:SysDictType:getByType:%s";
        /** 某类型下字典数据缓存。 */
        public static final String DICT_DATA_BY_TYPE = "admin:SysDictData:listByType:%s";

        /** 参数值缓存。 */
        public static final String CONFIG_BY_KEY = "admin:SysConfig:getByKey:%s";
        /** 已发布公告列表。 */
        public static final String NOTICE_PUBLISHED = "admin:SysNotice:listPublished";
        /** 用户第三方绑定列表。 */
        public static final String OAUTH_BY_USER = "admin:SysUserOauth:listByUserId:%s";
        /** 文件元数据缓存。 */
        public static final String FILE_BY_ID = "admin:SysFile:getById:%s";
    }

    /**
     * 线路模块（后续批次使用）。
     */
    public static final class Catalog {

        private Catalog() {
        }
    }

    /**
     * 订单模块（后续批次使用）。
     */
    public static final class Trade {

        private Trade() {
        }
    }

    /**
     * 客户模块（后续批次使用）。
     */
    public static final class Customer {

        private Customer() {
        }
    }

    /**
     * 运营模块（后续批次使用）。
     */
    public static final class Operations {

        private Operations() {
        }
    }

    /**
     * 通用分布式锁。
     */
    public static final class Lock {

        private Lock() {
        }

        /** 缓存重建互斥锁，参数为被重建的缓存 key。 */
        public static final String CACHE_REBUILD = "lock:cache:rebuild:%s";
    }
}
