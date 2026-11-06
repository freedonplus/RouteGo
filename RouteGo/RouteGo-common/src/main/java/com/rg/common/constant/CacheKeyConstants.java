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

        /** 预检权限获取。参数依次为：userId、版本号（全局版本:用户版本）。 */
        public static final String PERM_LIST="admin:config:SaToken:getPerm:%s:%s";

        /** 全局权限版本号。**只能自增**：删除会让版本回退，旧权限缓存重新命中。 */
        public static final String PERM_VERSION="admin:config:SaToken:getPermVer";

        /** 单用户权限版本号。参数为 userId，用于只失效某个用户的权限缓存。 */
        public static final String PERM_USER_VERSION = "admin:config:SaToken:getPermVer:%s";

        /** 图形验证码答案。 */
        public static final String CAPTCHA = "admin:Auth:captcha:%s";
        /** 登录失败计数。 */
        public static final String LOGIN_FAIL = "admin:Auth:loginFail:%s";
        /** 登录锁定标记。 */
        public static final String LOGIN_LOCK = "admin:Auth:loginLock:%s";

        /** 验证码答案。参数依次为：场景、接收方（手机号 / 邮箱）。 */
        public static final String VERIFY_CODE = "admin:Auth:code:%s:%s";
        /** 验证码重发冷却标记。参数依次为：场景、接收方。 */
        public static final String VERIFY_CODE_COOLDOWN = "admin:Auth:code:cd:%s:%s";
        /** 验证码校验失败次数。参数依次为：场景、接收方。 */
        public static final String VERIFY_CODE_ATTEMPT = "admin:Auth:code:try:%s:%s";
        /** 单个接收方每日发送计数。参数为接收方。 */
        public static final String VERIFY_CODE_DAILY = "admin:Auth:code:daily:%s";
        /** 单个 IP 每小时发送计数。参数为客户端 IP。 */
        public static final String VERIFY_CODE_IP = "admin:Auth:code:ip:%s";

        /** 第三方登录 state，防 CSRF。参数为 state 值。 */
        public static final String OAUTH_STATE = "admin:Auth:oauth:state:%s";

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
        /** IP 归属地查询结果。参数为 IP。 */
        public static final String IP_LOCATION = "admin:Ip:location:%s";
    }

    /**
     * 线路模块。
     */
    public static final class Catalog {

        private Catalog() {
        }

        /**
         * 线路详情聚合（主表 + 分类 + 图集 + 行程 + 标签）。参数为线路 ID。
         * <p>
         * 缓存里存的是库内相对路径，签名链接在出参时生成，不进缓存。
         */
        public static final String ROUTE_DETAIL = "catalog:route:detail:%s";
    }

    /**
     * 订单模块。
     */
    public static final class Trade {

        private Trade() {
        }

        /**
         * 下单幂等占位与结果。参数依次为：客户 ID、客户端幂等键。
         * 值为 {@code PENDING} 或 {@code DONE:{orderId}}。
         */
        public static final String ORDER_IDEM = "trade:order:idem:%s:%s";

        /**
         * 未传幂等键时的窗口去重键。参数依次为：客户 ID、线路 ID、班期 ID、出行人数。
         */
        public static final String ORDER_DUP = "trade:order:dup:%s:%s:%s:%s";
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

        /**
         * 客服自动分配的轮询游标，参数为规则 ID。
         * <p>
         * 纯自增计数，不设过期：规则删掉后 key 自然成为孤儿，由巡检脚本按前缀清理即可，
         * 这里不为了省一个 key 让轮询每次都从 0 开始（会导致总是分给同一个人）。
         */
        public static final String CS_ASSIGN_ROUND_ROBIN = "operations:cs:assign:rr:%s";
    }

    /**
     * 通用分布式锁。
     */
    public static final class Lock {

        private Lock() {
        }

        /** 缓存重建互斥锁，参数为被重建的缓存 key。 */
        public static final String CACHE_REBUILD = "lock:cache:rebuild:%s";

        /**
         * 班期维度下单锁，参数为班期 ID。
         * <p>
         * 加在事务外层：锁包住「事务开始 → 提交结束」，释放时数据已可见。
         */
        public static final String DEPARTURE = "lock:departure:%s";
    }
}
