-- =============================================================================
-- RouteGo 后台权限数据 · 标准初始化脚本（幂等）
-- 执行顺序：sql/admin-reset.sql → sql/admin-init.sql → 清 Redis admin:* 缓存
--
-- 本脚本写入：
--   1. 8 个内置角色（含「超级管理员」「系统管理员」）
--   2. 195 个接口级权限点，全部带中文说明（sys_permission.description）
--      覆盖 6 个域：admin 74 + catalog 40 + trade 23 + customer 26 + operations 32
--   3. 30 个菜单：5 个目录 + 25 个页面
--      系统管理 10 页、线路管理 5 页、订单管理 3 页、会员管理 3 页、运营中心 4 页
--   4. 角色 ↔ 菜单、角色 ↔ 权限点 的授权关系
--   5. 内置超级管理员账号：taishangxue520 / 12345678Ss
--      手机号 18807420597，邮箱 2120387318@qq.com
--   6. 10 类常用字典（33 个字典项）、8 条系统参数、3 条公告
--
-- 说明：
--   * 权限点 ID 1~195、菜单 ID 100~199 由本脚本固定占用，重跑会先删除再重建，
--     保证与后端 @SaCheckPermission 完全一致；用户自行新增的权限点（ID > 195）
--     不会被删除。
--   * 角色授权为「覆盖式」：重跑会把种子角色的授权恢复成脚本里的定义。
--   * 正式环境请在首次登录后立即修改内置账号密码。
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. 内置角色（共 5 个）
--    sort 越小越靠前；super_admin / admin 为受保护角色（代码中不可停用、不可删除）。
--    普通用户 / 会员用户是 App 端角色：注册默认绑定普通用户，不分配后台菜单与权限点。
-- ---------------------------------------------------------------------------
INSERT INTO sys_role (id, role_name, role_code, sort, status, remark, version, deleted)
VALUES (1, '超级管理员', 'super_admin', 1, 1,
        '平台最高权限，鉴权时直接返回通配权限 *，拥有全部菜单，不可删除', 0, 0),
       (2, '系统管理员', 'admin', 10, 1,
        '维护账号、角色、字典、参数、公告、文件与日志；不含权限点与菜单结构的增删改', 0, 0),
       (3, '客服', 'support', 20, 1,
        '处理工单、定制需求与订单客诉，可查用户资料与线路信息', 0, 0),
       (4, '普通用户', 'normal_user', 90, 1,
        '注册用户默认角色（App 端），不分配后台菜单与权限点', 0, 0),
       (5, '会员用户', 'member_user', 91, 1,
        '付费会员角色，预留会员体系扩展', 0, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name),
                        role_code = VALUES(role_code),
                        sort      = VALUES(sort),
                        status    = 1,
                        remark    = VALUES(remark),
                        deleted   = 0;

-- 历史遗留角色（系统运维/安全审计员/内容运营/数据分析师/只读访客等）清理
DELETE FROM sys_role_menu WHERE role_id > 5;
DELETE FROM sys_role_permission WHERE role_id > 5;
DELETE FROM sys_user_role WHERE role_id > 5;
DELETE FROM sys_role WHERE id > 5;

-- ---------------------------------------------------------------------------
-- 2. 权限点
--    命名规则：admin:<Controller>:<method>，必须与 Controller 上的 @SaCheckPermission 一致。
--    module 用于前端按模块分组与筛选，description 是该权限点的中文说明。
-- ---------------------------------------------------------------------------
-- 先清理种子区间（1~195）及其角色关联，保证权限码随代码同步
DELETE FROM sys_role_permission WHERE permission_id BETWEEN 1 AND 195;
DELETE FROM sys_permission WHERE id BETWEEN 1 AND 195;

INSERT INTO sys_permission (id, permission_name, permission_code, module, description, status, deleted)
VALUES
-- 工作台 ----------------------------------------------------------------
(1, '仪表盘概览', 'admin:Dashboard:overview', '工作台',
 '读取首页统计卡片、趋势图与待办数据，只读接口', 1, 0),

-- 系统配置 --------------------------------------------------------------
(2, '参数分页', 'admin:SysConfig:page', '系统配置',
 '分页查询系统参数，支持按参数名与参数键过滤（只读）', 1, 0),
(3, '按键读取参数', 'admin:SysConfig:getByKey', '系统配置',
 '按参数键读取单个参数值，供前端初始化与业务默认值使用（只读）', 1, 0),
(4, '参数详情', 'admin:SysConfig:detail', '系统配置',
 '按主键查看参数完整信息（只读）', 1, 0),
(5, '新增参数', 'admin:SysConfig:create', '系统配置',
 '新建系统参数；参数键全局唯一且创建后不可修改', 1, 0),
(6, '编辑参数', 'admin:SysConfig:update', '系统配置',
 '修改参数名称与参数值，保存后读取该参数的页面立即生效', 1, 0),
(7, '删除参数', 'admin:SysConfig:delete', '系统配置',
 '删除系统参数，业务读取将回退到代码默认值，属高危操作', 1, 0),
(8, '批量更新参数', 'admin:SysConfig:batchUpdate', '系统配置',
 '一次提交多条参数修改，供配置页统一保存', 1, 0),

(9, '字典类型分页', 'admin:SysDictType:page', '系统配置',
 '分页查询字典类型（只读）', 1, 0),
(10, '字典类型详情', 'admin:SysDictType:detail', '系统配置',
 '查看字典类型名称、编码与状态（只读）', 1, 0),
(11, '新增字典类型', 'admin:SysDictType:create', '系统配置',
 '新建字典类型；类型编码为小写字母/数字/下划线，创建后不可修改', 1, 0),
(12, '编辑字典类型', 'admin:SysDictType:update', '系统配置',
 '修改字典名称、状态与备注，类型编码不可变更', 1, 0),
(13, '删除字典类型', 'admin:SysDictType:delete', '系统配置',
 '删除字典类型，需先清理该类型下的字典项', 1, 0),
(14, '启用/禁用字典类型', 'admin:SysDictType:updateStatus', '系统配置',
 '停用后该类型下所有字典项不再对外提供', 1, 0),

(15, '字典数据分页', 'admin:SysDictData:page', '系统配置',
 '分页查询字典项（只读）', 1, 0),
(16, '字典数据详情', 'admin:SysDictData:detail', '系统配置',
 '查看单条字典项的标签、字典值与样式（只读）', 1, 0),
(17, '新增字典数据', 'admin:SysDictData:create', '系统配置',
 '在指定字典类型下新增字典项，同类型内字典值唯一', 1, 0),
(18, '编辑字典数据', 'admin:SysDictData:update', '系统配置',
 '修改字典项标签、字典值、排序与样式', 1, 0),
(19, '删除字典数据', 'admin:SysDictData:delete', '系统配置',
 '删除字典项，历史业务数据中已保存的值不受影响', 1, 0),
(20, '启用/禁用字典数据', 'admin:SysDictData:updateStatus', '系统配置',
 '控制单个字典项是否出现在前端下拉选项中', 1, 0),
(21, '设为默认项', 'admin:SysDictData:setDefault', '系统配置',
 '将某个字典项设为该类型的默认值，同类型仅一项生效', 1, 0),

-- 用户与权限 ------------------------------------------------------------
(22, '用户分页', 'admin:SysUser:page', '用户与权限',
 '分页查询后台用户，支持账号、手机号、邮箱、状态过滤（只读）', 1, 0),
(23, '用户详情', 'admin:SysUser:detail', '用户与权限',
 '查看用户资料、已绑定角色与最近登录信息（只读）', 1, 0),
(24, '新增用户', 'admin:SysUser:create', '用户与权限',
 '创建后台账号并设置初始密码，账号全局唯一', 1, 0),
(25, '编辑用户', 'admin:SysUser:update', '用户与权限',
 '修改用户昵称、手机号、邮箱与备注', 1, 0),
(26, '启用/禁用用户', 'admin:SysUser:updateStatus', '用户与权限',
 '禁用后该账号无法登录，已登录会话在下次鉴权时失效', 1, 0),
(27, '重置用户密码', 'admin:SysUser:resetPassword', '用户与权限',
 '为指定用户重置登录密码，重置后旧密码立即失效', 1, 0),
(28, '查询用户角色', 'admin:SysUser:listRoles', '用户与权限',
 '读取用户已绑定的角色 ID 列表（只读）', 1, 0),
(29, '分配用户角色', 'admin:SysUser:assignRoles', '用户与权限',
 '覆盖式设置用户角色，只能授予操作人自身已持有的角色', 1, 0),
(30, '删除用户', 'admin:SysUser:delete', '用户与权限',
 '逻辑删除账号并清理角色绑定，属高危操作', 1, 0),

(31, '角色分页', 'admin:SysRole:page', '用户与权限',
 '分页查询角色（只读）', 1, 0),
(32, '角色详情', 'admin:SysRole:detail', '用户与权限',
 '查看角色基础信息与用户数/权限数统计（只读）', 1, 0),
(33, '新增角色', 'admin:SysRole:create', '用户与权限',
 '新建角色，角色编码全局唯一且创建后不可修改', 1, 0),
(34, '编辑角色', 'admin:SysRole:update', '用户与权限',
 '修改角色名称、排序与备注，内置角色的编码受保护', 1, 0),
(35, '启用/禁用角色', 'admin:SysRole:updateStatus', '用户与权限',
 '停用后角色下用户不再获得该角色授权，内置角色不可停用', 1, 0),
(36, '删除角色', 'admin:SysRole:delete', '用户与权限',
 '删除角色并清理菜单/权限绑定；角色下有用户或为内置角色时禁止', 1, 0),
(37, '查询角色菜单', 'admin:SysRole:listMenus', '用户与权限',
 '读取角色已勾选的菜单 ID（只读）', 1, 0),
(38, '分配角色菜单', 'admin:SysRole:assignMenus', '用户与权限',
 '覆盖式设置角色可见菜单，决定侧边栏入口与可路由页面', 1, 0),
(39, '查询角色权限点', 'admin:SysRole:listPermissions', '用户与权限',
 '读取角色已勾选的权限点 ID（只读）', 1, 0),
(40, '分配角色权限点', 'admin:SysRole:assignPermissions', '用户与权限',
 '覆盖式设置角色权限点，只能授予操作人自身持有的权限点', 1, 0),
(41, '角色下用户分页', 'admin:SysRole:pageUsers', '用户与权限',
 '查看某角色已分配的用户列表（只读）', 1, 0),

(42, '权限点分页', 'admin:SysPermission:page', '用户与权限',
 '分页查询接口级权限点（只读）', 1, 0),
(43, '权限点分组', 'admin:SysPermission:grouped', '用户与权限',
 '按模块分组返回全部权限点，供角色授权抽屉使用（只读）', 1, 0),
(44, '权限点详情', 'admin:SysPermission:detail', '用户与权限',
 '查看权限点名称、编码、模块与说明（只读）', 1, 0),
(45, '新增权限点', 'admin:SysPermission:create', '用户与权限',
 '新建接口级权限点，权限码需与后端 @SaCheckPermission 保持一致', 1, 0),
(46, '编辑权限点', 'admin:SysPermission:update', '用户与权限',
 '修改权限名称、模块与说明，权限码受全局唯一约束保护', 1, 0),
(47, '删除权限点', 'admin:SysPermission:delete', '用户与权限',
 '删除权限点并解除角色关联，被角色引用时禁止删除', 1, 0),
(48, '启用/禁用权限点', 'admin:SysPermission:updateStatus', '用户与权限',
 '停用后即使角色已授权，接口鉴权也不再放行', 1, 0),

(49, '菜单树', 'admin:SysMenu:tree', '用户与权限',
 '读取全量菜单树，用于菜单维护与角色授权（只读）', 1, 0),
(50, '菜单详情', 'admin:SysMenu:detail', '用户与权限',
 '查看菜单路径、组件、图标与权限标识（只读）', 1, 0),
(51, '新增菜单', 'admin:SysMenu:create', '用户与权限',
 '新建目录/菜单/按钮，路径与组件需与前端路由一致', 1, 0),
(52, '编辑菜单', 'admin:SysMenu:update', '用户与权限',
 '修改菜单名称、路径、组件、图标与排序', 1, 0),
(53, '删除菜单', 'admin:SysMenu:delete', '用户与权限',
 '删除菜单并清理角色菜单绑定，存在子菜单时需先删除子级', 1, 0),
(54, '启用/禁用菜单', 'admin:SysMenu:updateStatus', '用户与权限',
 '停用后该菜单对所有角色隐藏且不可路由', 1, 0),
(55, '显示/隐藏菜单', 'admin:SysMenu:updateVisible', '用户与权限',
 '仅控制侧边栏显隐，不影响路由访问与接口鉴权', 1, 0),

(56, '用户绑定列表', 'admin:SysUserOauth:listByUser', '用户与权限',
 '查看用户已绑定的第三方账号（只读）', 1, 0),
(57, '绑定第三方账号', 'admin:SysUserOauth:bind', '用户与权限',
 '手工绑定第三方 openid，同一平台 openid 全局唯一', 1, 0),
(58, '解绑第三方账号', 'admin:SysUserOauth:unbind', '用户与权限',
 '解除第三方绑定，解绑后无法再用该平台登录', 1, 0),

-- 内容与文件 ------------------------------------------------------------
(59, '公告分页', 'admin:SysNotice:page', '内容与文件',
 '分页查询公告，含未发布草稿（只读）', 1, 0),
(60, '公告详情', 'admin:SysNotice:detail', '内容与文件',
 '查看公告正文、类型与发布状态（只读）', 1, 0),
(61, '新增公告', 'admin:SysNotice:create', '内容与文件',
 '新建公告，默认草稿状态，发布后才对用户可见', 1, 0),
(62, '编辑公告', 'admin:SysNotice:update', '内容与文件',
 '修改公告标题、正文、类型与置顶标记', 1, 0),
(63, '删除公告', 'admin:SysNotice:delete', '内容与文件',
 '删除公告，已发布内容立即下线', 1, 0),
(64, '发布公告', 'admin:SysNotice:publish', '内容与文件',
 '将草稿发布为可见状态并记录发布时间', 1, 0),
(65, '下架公告', 'admin:SysNotice:unpublish', '内容与文件',
 '撤回已发布公告，用户端不再展示', 1, 0),
(66, '置顶/取消置顶', 'admin:SysNotice:setTop', '内容与文件',
 '控制公告在列表中的置顶顺序', 1, 0),

(67, '上传文件', 'admin:SysFile:upload', '内容与文件',
 '上传图片/PDF 等附件，受扩展名白名单与大小上限限制', 1, 0),
(68, '文件分页', 'admin:SysFile:page', '内容与文件',
 '分页查询文件元数据（只读）', 1, 0),
(69, '文件详情', 'admin:SysFile:detail', '内容与文件',
 '查看文件名称、大小、类型与访问地址（只读）', 1, 0),
(70, '下载文件', 'admin:SysFile:download', '内容与文件',
 '下载文件内容，会记录访问审计', 1, 0),
(71, '删除文件', 'admin:SysFile:delete', '内容与文件',
 '删除文件记录与磁盘副本，不可恢复', 1, 0),

-- 日志审计 --------------------------------------------------------------
(72, '登录日志分页', 'admin:SysLoginLog:page', '日志审计',
 '分页查询登录成功/失败记录与 IP 归属地（只读）', 1, 0),
(73, '操作日志分页', 'admin:SysOperLog:page', '日志审计',
 '分页查询接口调用与数据变更审计记录（只读）', 1, 0),
(74, '操作日志详情', 'admin:SysOperLog:detail', '日志审计',
 '查看单次请求的入参、耗时与异常信息（只读）', 1, 0),

-- 线路管理 --------------------------------------------------------------
(75, '线路分页', 'catalog:Route:page', '线路管理',
 '分页查询线路，支持标题/编号/目的地/出发城市/分类/状态过滤（只读）', 1, 0),
(76, '线路详情', 'catalog:Route:detail', '线路管理',
 '查看线路完整信息，含分类名称、标签、图片与逐日行程（只读）', 1, 0),
(77, '新增线路', 'catalog:Route:create', '线路管理',
 '新建线路；线路编号留空时自动生成，编号全局唯一', 1, 0),
(78, '编辑线路', 'catalog:Route:update', '线路管理',
 '修改线路内容；线路编号、销量与评分不允许通过本接口修改', 1, 0),
(79, '线路上下架', 'catalog:Route:updateStatus', '线路管理',
 '上架后用户端可见并可下单，下架立即隐藏', 1, 0),
(80, '删除线路', 'catalog:Route:delete', '线路管理',
 '逻辑删除线路，同时清理其行程、图片与标签关联', 1, 0),

(81, '分类树', 'catalog:RouteCategory:tree', '线路管理',
 '读取完整分类树，用于分类维护与线路归类（只读）', 1, 0),
(82, '分类详情', 'catalog:RouteCategory:detail', '线路管理',
 '查看分类基础信息（只读）', 1, 0),
(83, '新增分类', 'catalog:RouteCategory:create', '线路管理',
 '新建线路分类，同一父级下名称不可重复', 1, 0),
(84, '编辑分类', 'catalog:RouteCategory:update', '线路管理',
 '修改分类名称/排序/父级，父级不能是自己或自己的子分类', 1, 0),
(85, '启用/禁用分类', 'catalog:RouteCategory:updateStatus', '线路管理',
 '停用后该分类不再用于新线路归类', 1, 0),
(86, '删除分类', 'catalog:RouteCategory:delete', '线路管理',
 '删除分类；存在子分类或已归类线路时禁止删除', 1, 0),

(87, '行程列表', 'catalog:RouteItinerary:list', '线路管理',
 '按天数升序读取某条线路的行程（只读）', 1, 0),
(88, '新增行程', 'catalog:RouteItinerary:create', '线路管理',
 '为线路新增某一天的行程，同线路内天数不可重复', 1, 0),
(89, '编辑行程', 'catalog:RouteItinerary:update', '线路管理',
 '修改单天行程的主题、餐住、交通与配图', 1, 0),
(90, '删除行程', 'catalog:RouteItinerary:delete', '线路管理',
 '删除单天行程，删除后可重新添加同一天', 1, 0),
(91, '覆盖保存行程', 'catalog:RouteItinerary:saveBatch', '线路管理',
 '整体覆盖线路行程，供行程编辑器一次性提交', 1, 0),

(92, '图片列表', 'catalog:RouteImage:list', '线路管理',
 '读取线路相册图片（只读）', 1, 0),
(93, '新增图片', 'catalog:RouteImage:create', '线路管理',
 '为线路相册添加图片', 1, 0),
(94, '删除图片', 'catalog:RouteImage:delete', '线路管理',
 '删除线路相册中的图片', 1, 0),

(95, '标签分页', 'catalog:RouteTag:page', '线路管理',
 '分页查询线路标签（只读）', 1, 0),
(96, '标签列表', 'catalog:RouteTag:list', '线路管理',
 '读取所有启用状态的标签，供线路编辑时选择（只读）', 1, 0),
(97, '新增标签', 'catalog:RouteTag:create', '线路管理',
 '新建标签，标签名称全局唯一', 1, 0),
(98, '编辑标签', 'catalog:RouteTag:update', '线路管理',
 '修改标签名称、颜色与排序', 1, 0),
(99, '启用/禁用标签', 'catalog:RouteTag:updateStatus', '线路管理',
 '停用后该标签不再出现在可选列表中', 1, 0),
(100, '删除标签', 'catalog:RouteTag:delete', '线路管理',
 '删除标签并清理它与线路的绑定关系', 1, 0),

(101, '线路标签查询', 'catalog:RouteTagRel:list', '线路管理',
 '查看某条线路已绑定的标签（只读）', 1, 0),
(102, '绑定线路标签', 'catalog:RouteTagRel:assign', '线路管理',
 '覆盖式设置线路标签，传空数组表示清空', 1, 0),
(103, '解绑线路标签', 'catalog:RouteTagRel:remove', '线路管理',
 '解除线路与某个标签的绑定', 1, 0),

(104, '班期分页', 'catalog:RouteDeparture:page', '线路管理',
 '分页查询出发班期，支持线路/状态/日期区间过滤（只读）', 1, 0),
(105, '班期列表', 'catalog:RouteDeparture:list', '线路管理',
 '按出发日期升序读取线路班期（只读）', 1, 0),
(106, '新增班期', 'catalog:RouteDeparture:create', '线路管理',
 '新建班期；同一线路同一天只能有一个班期', 1, 0),
(107, '编辑班期', 'catalog:RouteDeparture:update', '线路管理',
 '修改班期日期、价格与库存，库存不能小于已售数量', 1, 0),
(108, '停售/恢复班期', 'catalog:RouteDeparture:updateStatus', '线路管理',
 '控制班期是否可售，停售后用户端不可下单', 1, 0),
(109, '删除班期', 'catalog:RouteDeparture:delete', '线路管理',
 '删除班期；已产生成交时禁止删除，只能改为停售', 1, 0),

(110, '评价分页', 'catalog:RouteReview:page', '线路管理',
 '分页查询线路评价，支持线路/评分/状态过滤（只读）', 1, 0),
(111, '评价详情', 'catalog:RouteReview:detail', '线路管理',
 '查看评价内容、图片与商家回复（只读）', 1, 0),
(112, '回复评价', 'catalog:RouteReview:reply', '线路管理',
 '以商家身份回复评价并记录回复时间', 1, 0),
(113, '显示/隐藏评价', 'catalog:RouteReview:updateStatus', '线路管理',
 '隐藏后该评价不再在用户端展示', 1, 0),
(114, '删除评价', 'catalog:RouteReview:delete', '线路管理',
 '逻辑删除评价，属不可恢复操作', 1, 0),

-- 订单管理 --------------------------------------------------------------
(115, '订单分页', 'trade:Orders:page', '订单管理',
 '按订单号、客户、线路、状态、出发日期与下单时间筛选订单（只读）', 1, 0),
(116, '订单详情', 'trade:Orders:detail', '订单管理',
 '查看订单快照、出行人、状态流水、支付与退款记录（只读）', 1, 0),
(117, '后台代录订单', 'trade:Orders:create', '订单管理',
 '为电话/门店客户创建订单：按当前线路与班期生成快照并占用班期库存', 1, 0),
(118, '修改订单联系信息', 'trade:Orders:update', '订单管理',
 '修改联系人与备注；金额、线路与出发日期属于快照，不可修改', 1, 0),
(119, '取消订单', 'trade:Orders:cancel', '订单管理',
 '仅待支付订单可取消，取消后自动回补班期库存；已支付订单须走退款', 1, 0),
(120, '订单支付', 'trade:Orders:pay', '订单管理',
 '按订单实付金额收款并写支付记录，成功后累加线路销量与客户累计消费', 1, 0),
(121, '开始行程', 'trade:Orders:startTravel', '订单管理',
 '把已支付订单标记为出行中，用于区分未出行与行程进行中', 1, 0),
(122, '完成行程', 'trade:Orders:complete', '订单管理',
 '出行中订单转为已完成；完成后不可再修改名单，只能走退款', 1, 0),
(123, '删除订单', 'trade:Orders:delete', '订单管理',
 '逻辑删除终态订单（已完成/已取消/已退款），在办订单禁止删除', 1, 0),

-- 订单出行人 ------------------------------------------------------------
(124, '出行人列表', 'trade:OrderTraveler:listByOrder', '订单出行人',
 '按订单查看出行人名单，证件号与手机号脱敏展示（只读）', 1, 0),
(125, '新增出行人', 'trade:OrderTraveler:create', '订单出行人',
 '向未完结订单追加一位出行人，证件号与手机号加密存储', 1, 0),
(126, '修改出行人', 'trade:OrderTraveler:update', '订单出行人',
 '修改出行人姓名、证件号、性别与类型', 1, 0),
(127, '删除出行人', 'trade:OrderTraveler:delete', '订单出行人',
 '从订单名单中移除一位出行人，不改变订单人数与金额', 1, 0),
(128, '覆盖保存出行人', 'trade:OrderTraveler:replaceAll', '订单出行人',
 '整单覆盖式保存：先清空现有名单，再按本次提交重建', 1, 0),

-- 订单状态流水 ----------------------------------------------------------
(129, '状态流水分页', 'trade:OrderStatusLog:page', '订单状态流水',
 '按订单、目标状态、操作人与时间查询流转台账（只读）', 1, 0),
(130, '订单流水', 'trade:OrderStatusLog:listByOrder', '订单状态流水',
 '按订单查看完整状态变更历史，首条为下单（只读）', 1, 0),

-- 支付记录 --------------------------------------------------------------
(131, '支付记录分页', 'trade:PaymentRecord:page', '支付记录',
 '查询支付流水，支持按支付单号、支付方式与支付时间筛选（只读）', 1, 0),
(132, '订单支付记录', 'trade:PaymentRecord:listByOrder', '支付记录',
 '按订单查看支付记录，含第三方交易号（只读）', 1, 0),

-- 退款管理 --------------------------------------------------------------
(133, '退款记录分页', 'trade:RefundRecord:page', '退款管理',
 '查询退款单，支持按状态与申请时间筛选（只读）', 1, 0),
(134, '退款详情', 'trade:RefundRecord:detail', '退款管理',
 '查看退款金额、原因与审核意见（只读）', 1, 0),
(135, '订单退款记录', 'trade:RefundRecord:listByOrder', '退款管理',
 '按订单查看历史退款记录（只读）', 1, 0),
(136, '申请退款', 'trade:RefundRecord:apply', '退款管理',
 '对已支付订单发起退款：金额不得超过实付金额，申请后订单转为退款中', 1, 0),
(137, '退款审核', 'trade:RefundRecord:audit', '退款管理',
 '审核退款：通过后全额退款订单转已退款并回补库存与统计，驳回则退回已支付', 1, 0),

-- 会员管理 --------------------------------------------------------------
(138, '会员分页', 'customer:Customer:page', '会员管理',
 '按手机号精确或昵称/姓名模糊查询会员，手机号一律显示脱敏值', 1, 0),
(139, '会员详情', 'customer:Customer:detail', '会员管理',
 '查看会员资料、等级、积分、累计消费与可用/已用券数量', 1, 0),
(140, '新增会员', 'customer:Customer:create', '会员管理',
 '后台代注册会员：手机号唯一，密码可留空（仅支持验证码登录）', 1, 0),
(141, '编辑会员', 'customer:Customer:update', '会员管理',
 '修改昵称、头像、真实姓名、证件号、性别、生日与邮箱；不含手机号、密码与积分', 1, 0),
(142, '手机号换绑', 'customer:Customer:updatePhone', '会员管理',
 '更换会员手机号，同时更新密文列与盲索引列，新号码不能与其他会员重复', 1, 0),
(143, '重置会员密码', 'customer:Customer:resetPassword', '会员管理',
 '为会员设置新的登录密码（BCrypt 加密存储），操作会记入操作日志', 1, 0),
(144, '会员状态变更', 'customer:Customer:updateStatus', '会员管理',
 '禁用后会员无法登录，已领取的优惠券不受影响；启用即恢复登录', 1, 0),
(145, '会员等级调整', 'customer:Customer:updateLevel', '会员管理',
 '手动调整会员等级：普通 / 银卡 / 金卡 / 钻石', 1, 0),
(146, '会员积分调整', 'customer:Customer:adjustPoints', '会员管理',
 '按增减量调整积分，扣减后不会小于 0，用于补偿或活动发放', 1, 0),
(147, '会员券包', 'customer:Customer:listCoupons', '会员管理',
 '查看该会员名下全部优惠券及使用状态（只读）', 1, 0),
(148, '删除会员', 'customer:Customer:delete', '会员管理',
 '逻辑删除会员并作废其未使用的券；历史订单保留，不受影响', 1, 0),

-- 优惠券模板 ------------------------------------------------------------
(149, '券模板分页', 'customer:CouponTemplate:page', '优惠券模板',
 '查询券模板，支持按名称、类型与状态筛选（只读）', 1, 0),
(150, '可用券模板列表', 'customer:CouponTemplate:list', '优惠券模板',
 '供发券下拉框使用，自动排除已停用与已过有效期的模板', 1, 0),
(151, '券模板详情', 'customer:CouponTemplate:detail', '优惠券模板',
 '查看券面规则、发行量、已发数量与剩余数量（只读）', 1, 0),
(152, '新增券模板', 'customer:CouponTemplate:create', '优惠券模板',
 '满减券填减免金额、折扣券填折扣率；有效期支持固定区间或领取后 N 天', 1, 0),
(153, '编辑券模板', 'customer:CouponTemplate:update', '优惠券模板',
 '修改券面规则；修改有效期会同时影响已发出但未使用的券', 1, 0),
(154, '券模板状态变更', 'customer:CouponTemplate:updateStatus', '优惠券模板',
 '停用后不能再发放，已发出的券仍可正常使用；已过有效期的模板不能启用', 1, 0),
(155, '批量发券', 'customer:CouponTemplate:grant', '优惠券模板',
 '给指定会员批量发放该券，整批事务：任一会员不可发放则全部回滚', 1, 0),
(156, '删除券模板', 'customer:CouponTemplate:delete', '优惠券模板',
 '仅从未发放过的模板可删除；已发过券的模板只能停用', 1, 0),

-- 用户优惠券 ------------------------------------------------------------
(157, '优惠券分页', 'customer:Coupon:page', '用户优惠券',
 '按会员、券模板、券码、状态与领取时间查询已发出的券（只读）', 1, 0),
(158, '优惠券详情', 'customer:Coupon:detail', '用户优惠券',
 '查看券面、持有人、领取与使用记录（只读）', 1, 0),
(159, '按会员查券', 'customer:Coupon:list', '用户优惠券',
 '查看指定会员的券包，可按未使用/已使用/已过期过滤（只读）', 1, 0),
(160, '核销优惠券', 'customer:Coupon:use', '用户优惠券',
 '把券置为已使用；传订单号时校验订单归属、状态与使用门槛，重复核销返回冲突', 1, 0),
(161, '作废优惠券', 'customer:Coupon:revoke', '用户优惠券',
 '仅未使用的券可作废；已使用的券需通过订单退款流程处理', 1, 0),
(162, '删除优惠券', 'customer:Coupon:delete', '用户优惠券',
 '删除券记录；已使用的券是核销凭证，不允许删除', 1, 0),
(163, '刷新过期状态', 'customer:Coupon:refreshExpired', '用户优惠券',
 '按模板有效期规则，把已过期但仍标记为未使用的券批量置为「已过期」', 1, 0),

-- 客服工单 ----------------------------------------------------------------
(164, '工单分页', 'operations:CsTicket:page', '客服工单',
 '按工单号、标题关键词、客户、订单、类型、优先级、状态、处理人与创建时间查询工单（只读）', 1, 0),
(165, '工单详情', 'operations:CsTicket:detail', '客服工单',
 '查看单个工单的完整信息与消息条数，完整对话走「工单对话」接口（只读）', 1, 0),
(166, '新建工单', 'operations:CsTicket:create', '客服工单',
 '受理客户咨询并建单；传了订单会自动带出下单会员，指派了处理人则直接进入「处理中」', 1, 0),
(167, '编辑工单', 'operations:CsTicket:update', '客服工单',
 '修改关联客户/订单、类型、标题与问题描述；已关闭的工单不可编辑', 1, 0),
(168, '指派处理人', 'operations:CsTicket:assign', '客服工单',
 '把工单指派给某个后台账号；待处理的工单会随指派自动进入「处理中」，已关闭的不可改派', 1, 0),
(169, '调整优先级', 'operations:CsTicket:updatePriority', '客服工单',
 '在 1 低 / 2 中 / 3 高之间调整优先级，用于插队处理紧急客诉；已关闭的不可调整', 1, 0),
(170, '工单状态流转', 'operations:CsTicket:updateStatus', '客服工单',
 '按状态机流转：待处理→处理中→已解决→已关闭，支持已解决重新打开；备注会写入工单消息留痕', 1, 0),
(171, '删除工单', 'operations:CsTicket:delete', '客服工单',
 '逻辑删除工单并物理清理其对话消息，不可恢复；历史沟通记录请先导出再删除', 1, 0),
(172, '工单看板统计', 'operations:CsTicket:statistics', '客服工单',
 '按状态与类型统计工单量，并给出未指派数、高优先级待处理数，供客服组长排班（只读）', 1, 0),

-- 工单消息 ----------------------------------------------------------------
(173, '消息分页', 'operations:CsMessage:page', '工单消息',
 '按工单、发送方与时间范围分页查询沟通记录，倒序返回（只读）', 1, 0),
(174, '工单对话', 'operations:CsMessage:listByTicket', '工单消息',
 '按时间正序返回某工单的完整对话，供详情页时间线渲染（只读）', 1, 0),
(175, '回复工单', 'operations:CsMessage:create', '工单消息',
 '以当前登录客服身份发送消息，可附图片；待处理工单会在回复后自动转为「处理中」，已关闭的拒绝发送', 1, 0),

-- 定制需求 ----------------------------------------------------------------
(176, '需求分页', 'operations:CustomRequirement:page', '定制需求',
 '按需求单号、联系人/目的地/偏好关键词、会员、状态、跟进人与日期查询（只读）', 1, 0),
(177, '需求详情', 'operations:CustomRequirement:detail', '定制需求',
 '查看需求全貌，附带报价版本数与最新一版报价金额、状态（只读）', 1, 0),
(178, '新建需求', 'operations:CustomRequirement:create', '定制需求',
 '录入定制游需求，联系电话加密落库、出口脱敏；可关联已注册会员，新单状态为「待跟进」', 1, 0),
(179, '编辑需求', 'operations:CustomRequirement:update', '定制需求',
 '修改联系人、目的地、日期、人数、预算与偏好；联系电话留空表示不修改；已关闭的不可编辑', 1, 0),
(180, '指派跟进人', 'operations:CustomRequirement:assign', '定制需求',
 '把需求分配给定制师跟进；已关闭的需求不可指派', 1, 0),
(181, '需求状态变更', 'operations:CustomRequirement:updateStatus', '定制需求',
 '仅支持改为 2 已成交（线下成交）或 3 已关闭；已有报价的需求必须走采纳流程，避免状态与报价对不上', 1, 0),
(182, '删除需求', 'operations:CustomRequirement:delete', '定制需求',
 '删除需求并连带清理其报价版本；已成交的需求是业务凭据，不允许删除只能关闭', 1, 0),

-- 定制报价 ----------------------------------------------------------------
(183, '报价分页', 'operations:CustomQuote:page', '定制报价',
 '按需求、报价单号、状态与创建时间查询报价版本；可只看已过有效期仍待确认的报价（只读）', 1, 0),
(184, '报价详情', 'operations:CustomQuote:detail', '定制报价',
 '查看单版报价的金额、明细、有效期与状态（只读）', 1, 0),
(185, '需求报价版本', 'operations:CustomQuote:listByRequirement', '定制报价',
 '查看某需求的全部报价版本，按版本从新到旧，供需求详情页的报价时间线使用（只读）', 1, 0),
(186, '新建报价', 'operations:CustomQuote:create', '定制报价',
 '为需求新增一版报价；需求若还是「待跟进」会自动推进到「已报价」，已成交或已关闭的拒绝报价', 1, 0),
(187, '修改报价', 'operations:CustomQuote:update', '定制报价',
 '仅「待确认」的报价可改金额、明细与有效期；调价请新建一版，已采纳的报价不允许改动', 1, 0),
(188, '采纳报价', 'operations:CustomQuote:accept', '定制报价',
 '把本单置为已采纳，同需求其它待确认报价自动失效，需求推进到已成交；整体一个事务', 1, 0),
(189, '作废报价', 'operations:CustomQuote:invalidate', '定制报价',
 '把待确认的报价置为已失效；已采纳的报价是成交凭据，不能作废', 1, 0),
(190, '删除报价', 'operations:CustomQuote:delete', '定制报价',
 '物理删除待确认的报价，不可恢复；已采纳或已失效的报价不允许删除', 1, 0),

-- 经营统计 ----------------------------------------------------------------
(191, '统计分页', 'operations:StatDaily:page', '经营统计',
 '按统计日期倒序查询每日经营数据，可按日期区间筛选（只读）', 1, 0),
(192, '单日明细', 'operations:StatDaily:detail', '经营统计',
 '查看某一天的订单数、支付额、退款额、净收入与新增会员，未生成统计的日期返回 404（只读）', 1, 0),
(193, '区间汇总', 'operations:StatDaily:summary', '经营统计',
 '汇总任意区间的经营指标，直接聚合订单、支付、退款与会员原始流水，不依赖报表是否已重算（只读）', 1, 0),
(194, '趋势数据', 'operations:StatDaily:trend', '经营统计',
 '按日期升序返回逐日指标，供首页与报表页折线图使用（只读）', 1, 0),
(195, '重算统计', 'operations:StatDaily:rebuild', '经营统计',
 '按天覆盖重算指定区间，幂等可重复执行；单次最多 366 天，用于补跑漏掉的定时任务', 1, 0),

-- 客服自动分配规则 -----------------------------------------------------------
(196, '规则分页', 'operations:CsAssignRule:page', '自动分配规则',
 '查看分配规则、启停状态与启用顺序，并用试算接口预演某类工单会分给谁（只读）', 1, 0),
(197, '新建规则', 'operations:CsAssignRule:create', '自动分配规则',
 '按工单类型/优先级圈定一组客服，选择轮询或「在处理最少」策略；至少一个可用客服才允许保存', 1, 0),
(198, '编辑规则', 'operations:CsAssignRule:update', '自动分配规则',
 '修改匹配条件、候选客服、策略与排序；带乐观锁，并发修改会提示刷新后重试', 1, 0),
(199, '删除规则', 'operations:CsAssignRule:delete', '自动分配规则',
 '逻辑删除规则，之后的新工单不再命中；已分配工单的 assign_rule_id 仍可追溯', 1, 0),
(200, '启停规则', 'operations:CsAssignRule:updateStatus', '自动分配规则',
 '临时停用某条规则而不删除，常用于客服请假期间把该组规则摘下', 1, 0);

-- ---------------------------------------------------------------------------
-- 3. 菜单（M 目录 / C 菜单；component 需与 src/router/menu-components.js 对应）
-- ---------------------------------------------------------------------------
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 100 AND 199;
DELETE FROM sys_menu WHERE id BETWEEN 100 AND 199;

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon,
                      sort, visible, status, version, deleted)
VALUES (100, 0, '系统管理', 'M', '/system', NULL, NULL, 'Settings', 1, 1, 1, 0, 0),

       (101, 100, '用户管理', 'C', '/system/user', 'system/user/index',
        'admin:SysUser:page', 'User', 1, 1, 1, 0, 0),
       (102, 100, '角色管理', 'C', '/system/role', 'system/role/index',
        'admin:SysRole:page', 'Team', 2, 1, 1, 0, 0),
       (103, 100, '菜单管理', 'C', '/system/menu', 'system/menu/index',
        'admin:SysMenu:tree', 'Menu', 3, 1, 1, 0, 0),
       (104, 100, '权限点', 'C', '/system/permission', 'system/permission/index',
        'admin:SysPermission:page', 'Key', 4, 1, 1, 0, 0),
       (105, 100, '字典管理', 'C', '/system/dict', 'system/dict/index',
        'admin:SysDictType:page', 'Book', 5, 1, 1, 0, 0),
       (106, 100, '参数配置', 'C', '/system/config', 'system/config/index',
        'admin:SysConfig:page', 'SetUp', 6, 1, 1, 0, 0),
       (107, 100, '公告管理', 'C', '/system/notice', 'system/notice/index',
        'admin:SysNotice:page', 'Bell', 7, 1, 1, 0, 0),
       (108, 100, '文件管理', 'C', '/system/file', 'system/file/index',
        'admin:SysFile:page', 'Folder', 8, 1, 1, 0, 0),
       (109, 100, '操作日志', 'C', '/system/oper-log', 'system/operlog/index',
        'admin:SysOperLog:page', 'Document', 9, 1, 1, 0, 0),
       (110, 100, '登录日志', 'C', '/system/login-log', 'system/loginlog/index',
        'admin:SysLoginLog:page', 'Clock', 10, 1, 1, 0, 0);

-- 业务模块菜单：线路 / 订单 / 会员 / 运营，路径与前端 menu-components 一一对应
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon,
                      sort, visible, status, version, deleted)
VALUES (111, 0, '线路管理', 'M', '/catalog', NULL, NULL, 'Guide', 2, 1, 1, 0, 0),

       (112, 111, '线路列表', 'C', '/catalog/route', 'catalog/route/index',
        'catalog:Route:page', 'Place', 1, 1, 1, 0, 0),
       (113, 111, '线路分类', 'C', '/catalog/category', 'catalog/category/index',
        'catalog:RouteCategory:tree', 'Collection', 2, 1, 1, 0, 0),
       (114, 111, '线路标签', 'C', '/catalog/tag', 'catalog/tag/index',
        'catalog:RouteTag:page', 'PriceTag', 3, 1, 1, 0, 0),
       (115, 111, '出发班期', 'C', '/catalog/departure', 'catalog/departure/index',
        'catalog:RouteDeparture:page', 'Van', 4, 1, 1, 0, 0),
       (116, 111, '线路评价', 'C', '/catalog/review', 'catalog/review/index',
        'catalog:RouteReview:page', 'Star', 5, 1, 1, 0, 0),

       (120, 0, '订单管理', 'M', '/trade', NULL, NULL, 'Tickets', 3, 1, 1, 0, 0),

       (121, 120, '订单列表', 'C', '/trade/order', 'trade/order/index',
        'trade:Orders:page', 'ShoppingCart', 1, 1, 1, 0, 0),
       (122, 120, '退款管理', 'C', '/trade/refund', 'trade/refund/index',
        'trade:RefundRecord:page', 'Money', 2, 1, 1, 0, 0),
       (123, 120, '支付流水', 'C', '/trade/payment', 'trade/payment/index',
        'trade:PaymentRecord:page', 'Wallet', 3, 1, 1, 0, 0),

       (130, 0, '会员管理', 'M', '/customer', NULL, NULL, 'Team', 4, 1, 1, 0, 0),

       (131, 130, '会员列表', 'C', '/customer/member', 'customer/customer/index',
        'customer:Customer:page', 'User', 1, 1, 1, 0, 0),
       (132, 130, '优惠券模板', 'C', '/customer/coupon-template', 'customer/coupon-template/index',
        'customer:CouponTemplate:page', 'Discount', 2, 1, 1, 0, 0),
       (133, 130, '用户优惠券', 'C', '/customer/coupon', 'customer/coupon/index',
        'customer:Coupon:page', 'Present', 3, 1, 1, 0, 0),

       (140, 0, '运营中心', 'M', '/operations', NULL, NULL, 'Service', 5, 1, 1, 0, 0),

       (141, 140, '客服工单', 'C', '/operations/ticket', 'operations/ticket/index',
        'operations:CsTicket:page', 'ChatDotRound', 1, 1, 1, 0, 0),
       (142, 140, '定制需求', 'C', '/operations/requirement', 'operations/requirement/index',
        'operations:CustomRequirement:page', 'Document', 2, 1, 1, 0, 0),
       (143, 140, '定制报价', 'C', '/operations/quote', 'operations/quote/index',
        'operations:CustomQuote:page', 'Tickets', 3, 1, 1, 0, 0),
       (144, 140, '经营统计', 'C', '/operations/stat', 'operations/stat/index',
        'operations:StatDaily:page', 'TrendCharts', 4, 1, 1, 0, 0);

-- ---------------------------------------------------------------------------
-- 4. 角色 ↔ 菜单（覆盖式）
-- ---------------------------------------------------------------------------
DELETE FROM sys_role_menu WHERE role_id BETWEEN 1 AND 50;

-- 超级管理员、系统管理员：全部菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
         JOIN sys_menu m ON m.deleted = 0
WHERE r.id IN (1, 2)
  AND m.id BETWEEN 100 AND 199;

-- 客服：用户/公告/文件 + 线路查询、订单、会员、工单与定制
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 3, id FROM sys_menu
WHERE id IN (100, 101, 107, 108,
             111, 112, 116,
             120, 121, 122, 123,
             130, 131, 133,
             140, 141, 142, 143);

-- 普通用户 / 会员用户：App 端角色，不分配后台菜单

-- ---------------------------------------------------------------------------
-- 5. 角色 ↔ 权限点（覆盖式）
-- ---------------------------------------------------------------------------
DELETE FROM sys_role_permission WHERE role_id BETWEEN 1 AND 8;

-- 超级管理员：全部权限点（鉴权时还会直接命中通配符 *，此处授权仅用于界面展示）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE deleted = 0;

-- 系统管理员：除「权限点」与「菜单」的结构变更外的全部权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id
FROM sys_permission
WHERE deleted = 0
  AND permission_code NOT IN ('admin:SysPermission:create',
                              'admin:SysPermission:update',
                              'admin:SysPermission:delete',
                              'admin:SysPermission:updateStatus',
                              'admin:SysMenu:create',
                              'admin:SysMenu:update',
                              'admin:SysMenu:delete',
                              'admin:SysMenu:updateStatus',
                              'admin:SysMenu:updateVisible');

-- 系统运维
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id
FROM sys_permission
WHERE permission_code IN ('admin:Dashboard:overview',
                          'admin:SysConfig:page', 'admin:SysConfig:getByKey', 'admin:SysConfig:detail',
                          'admin:SysConfig:create', 'admin:SysConfig:update', 'admin:SysConfig:delete',
                          'admin:SysConfig:batchUpdate',
                          'admin:SysDictType:page', 'admin:SysDictType:detail', 'admin:SysDictType:create',
                          'admin:SysDictType:update', 'admin:SysDictType:delete',
                          'admin:SysDictType:updateStatus',
                          'admin:SysDictData:page', 'admin:SysDictData:detail', 'admin:SysDictData:create',
                          'admin:SysDictData:update', 'admin:SysDictData:delete',
                          'admin:SysDictData:updateStatus', 'admin:SysDictData:setDefault',
                          'admin:SysFile:upload', 'admin:SysFile:page', 'admin:SysFile:detail',
                          'admin:SysFile:download', 'admin:SysFile:delete',
                          'admin:SysNotice:page', 'admin:SysNotice:detail', 'admin:SysNotice:create',
                          'admin:SysNotice:update', 'admin:SysNotice:delete', 'admin:SysNotice:publish',
                          'admin:SysNotice:unpublish', 'admin:SysNotice:setTop',
                          'admin:SysLoginLog:page',
                          'admin:SysOperLog:page', 'admin:SysOperLog:detail',
                          'admin:SysUser:page', 'admin:SysUser:detail',
                          'admin:SysRole:page',
                          'catalog:Route:page', 'catalog:Route:detail',
                          'catalog:RouteCategory:tree',
                          'catalog:RouteTag:list', 'catalog:RouteTag:page',
                          'catalog:RouteDeparture:page', 'catalog:RouteDeparture:list',
                          'catalog:RouteReview:page', 'catalog:RouteReview:detail',
                          'trade:Orders:page', 'trade:Orders:detail',
                          'trade:OrderStatusLog:page', 'trade:OrderStatusLog:listByOrder',
                          'trade:PaymentRecord:page', 'trade:PaymentRecord:listByOrder',
                          'trade:RefundRecord:page', 'trade:RefundRecord:detail',
                          'trade:RefundRecord:listByOrder',
                          'customer:Customer:page', 'customer:Customer:detail',
                          'customer:Customer:listCoupons',
                          'customer:CouponTemplate:page', 'customer:CouponTemplate:list',
                          'customer:CouponTemplate:detail',
                          'customer:Coupon:page', 'customer:Coupon:detail', 'customer:Coupon:list',
                          'customer:Coupon:refreshExpired',
                          'operations:CsTicket:page', 'operations:CsTicket:detail',
                          'operations:CsTicket:statistics',
                          'operations:StatDaily:page', 'operations:StatDaily:detail',
                          'operations:StatDaily:summary', 'operations:StatDaily:trend');

-- 安全审计员（只读）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, id
FROM sys_permission
WHERE permission_code IN ('admin:Dashboard:overview',
                          'admin:SysLoginLog:page',
                          'admin:SysOperLog:page', 'admin:SysOperLog:detail',
                          'admin:SysUser:page', 'admin:SysUser:detail',
                          'admin:SysRole:page', 'admin:SysRole:detail',
                          'admin:SysPermission:page', 'admin:SysPermission:grouped',
                          'admin:SysPermission:detail',
                          'admin:SysMenu:tree', 'admin:SysMenu:detail',
                          'trade:Orders:page', 'trade:Orders:detail',
                          'trade:OrderStatusLog:page', 'trade:OrderStatusLog:listByOrder',
                          'trade:PaymentRecord:page', 'trade:PaymentRecord:listByOrder',
                          'trade:RefundRecord:page', 'trade:RefundRecord:detail',
                          'trade:RefundRecord:listByOrder', 'trade:RefundRecord:audit',
                          'customer:Customer:page', 'customer:Customer:detail',
                          'customer:Customer:listCoupons',
                          'customer:CouponTemplate:page', 'customer:CouponTemplate:detail',
                          'customer:Coupon:page', 'customer:Coupon:detail', 'customer:Coupon:list',
                          'operations:CsTicket:page', 'operations:CsTicket:detail',
                          'operations:CsMessage:page', 'operations:CsMessage:listByTicket',
                          'operations:CsAssignRule:page',
                          'operations:CustomRequirement:page', 'operations:CustomRequirement:detail',
                          'operations:CustomQuote:page', 'operations:CustomQuote:detail',
                          'operations:CustomQuote:listByRequirement',
                          'operations:StatDaily:page', 'operations:StatDaily:detail',
                          'operations:StatDaily:summary', 'operations:StatDaily:trend');

-- 内容运营
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 5, id
FROM sys_permission
WHERE permission_code IN ('admin:Dashboard:overview',
                          'admin:SysNotice:page', 'admin:SysNotice:detail', 'admin:SysNotice:create',
                          'admin:SysNotice:update', 'admin:SysNotice:delete', 'admin:SysNotice:publish',
                          'admin:SysNotice:unpublish', 'admin:SysNotice:setTop',
                          'admin:SysFile:upload', 'admin:SysFile:page', 'admin:SysFile:detail',
                          'admin:SysFile:download', 'admin:SysFile:delete',
                          'customer:Customer:page', 'customer:Customer:detail',
                          'customer:Customer:listCoupons',
                          'customer:CouponTemplate:page', 'customer:CouponTemplate:list',
                          'customer:CouponTemplate:detail', 'customer:CouponTemplate:create',
                          'customer:CouponTemplate:update', 'customer:CouponTemplate:updateStatus',
                          'customer:CouponTemplate:grant',
                          'customer:Coupon:page', 'customer:Coupon:detail', 'customer:Coupon:list');

-- 客服专员
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 6, id
FROM sys_permission
WHERE permission_code IN ('admin:Dashboard:overview',
                          'admin:SysUser:page', 'admin:SysUser:detail',
                          'admin:SysUserOauth:listByUser',
                          'admin:SysNotice:page', 'admin:SysNotice:detail',
                          'admin:SysFile:page', 'admin:SysFile:detail', 'admin:SysFile:download',
                          'catalog:Route:page', 'catalog:Route:detail',
                          'catalog:RouteItinerary:list', 'catalog:RouteImage:list',
                          'catalog:RouteTagRel:list',
                          'catalog:RouteDeparture:list',
                          'catalog:RouteReview:page', 'catalog:RouteReview:detail',
                          'trade:Orders:page', 'trade:Orders:detail',
                          'trade:Orders:create', 'trade:Orders:update',
                          'trade:Orders:cancel', 'trade:Orders:pay',
                          'trade:Orders:startTravel', 'trade:Orders:complete',
                          'trade:OrderTraveler:listByOrder', 'trade:OrderTraveler:create',
                          'trade:OrderTraveler:update', 'trade:OrderTraveler:delete',
                          'trade:OrderTraveler:replaceAll',
                          'trade:OrderStatusLog:page', 'trade:OrderStatusLog:listByOrder',
                          'trade:PaymentRecord:page', 'trade:PaymentRecord:listByOrder',
                          'trade:RefundRecord:page', 'trade:RefundRecord:detail',
                          'trade:RefundRecord:listByOrder', 'trade:RefundRecord:apply',
                          'customer:Customer:page', 'customer:Customer:detail',
                          'customer:Customer:update', 'customer:Customer:updatePhone',
                          'customer:Customer:listCoupons',
                          'customer:CouponTemplate:page', 'customer:CouponTemplate:list',
                          'customer:CouponTemplate:detail',
                          'customer:Coupon:page', 'customer:Coupon:detail', 'customer:Coupon:list',
                          'customer:Coupon:use', 'customer:Coupon:revoke',
                          'operations:CsTicket:page', 'operations:CsTicket:detail',
                          'operations:CsTicket:create', 'operations:CsTicket:update',
                          'operations:CsTicket:assign', 'operations:CsTicket:updatePriority',
                          'operations:CsTicket:updateStatus',
                          'operations:CsMessage:page', 'operations:CsMessage:listByTicket',
                          'operations:CsMessage:create',
                          'operations:CustomRequirement:page', 'operations:CustomRequirement:detail',
                          'operations:CustomRequirement:create', 'operations:CustomRequirement:update',
                          'operations:CustomRequirement:assign',
                          'operations:CustomRequirement:updateStatus',
                          'operations:CustomQuote:page', 'operations:CustomQuote:detail',
                          'operations:CustomQuote:listByRequirement', 'operations:CustomQuote:create',
                          'operations:CustomQuote:update', 'operations:CustomQuote:accept',
                          'operations:CustomQuote:invalidate');

-- 数据分析师（只读）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 7, id
FROM sys_permission
WHERE permission_code IN ('admin:Dashboard:overview',
                          'admin:SysLoginLog:page',
                          'admin:SysOperLog:page', 'admin:SysOperLog:detail',
                          'admin:SysUser:page',
                          'admin:SysFile:page',
                          'admin:SysConfig:page',
                          'admin:SysDictType:page',
                          'catalog:Route:page',
                          'catalog:RouteCategory:tree',
                          'catalog:RouteDeparture:page',
                          'catalog:RouteReview:page',
                          'trade:Orders:page', 'trade:Orders:detail',
                          'trade:OrderStatusLog:page', 'trade:PaymentRecord:page',
                          'trade:RefundRecord:page',
                          'customer:Customer:page', 'customer:Customer:detail',
                          'customer:CouponTemplate:page',
                          'customer:Coupon:page', 'customer:Coupon:detail',
                          'operations:CsTicket:page', 'operations:CsTicket:detail',
                          'operations:CsTicket:statistics',
                          'operations:CustomRequirement:page', 'operations:CustomRequirement:detail',
                          'operations:CustomQuote:page', 'operations:CustomQuote:detail',
                          'operations:CustomQuote:listByRequirement',
                          'operations:StatDaily:page', 'operations:StatDaily:detail',
                          'operations:StatDaily:summary', 'operations:StatDaily:trend');

-- 只读访客：所有查询类权限，无任何写操作
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 8, id
FROM sys_permission
WHERE permission_code IN ('admin:Dashboard:overview',
                          'admin:SysConfig:page', 'admin:SysConfig:getByKey', 'admin:SysConfig:detail',
                          'admin:SysDictType:page', 'admin:SysDictType:detail',
                          'admin:SysDictData:page', 'admin:SysDictData:detail',
                          'admin:SysFile:page', 'admin:SysFile:detail', 'admin:SysFile:download',
                          'admin:SysLoginLog:page',
                          'admin:SysMenu:tree', 'admin:SysMenu:detail',
                          'admin:SysNotice:page', 'admin:SysNotice:detail',
                          'admin:SysOperLog:page', 'admin:SysOperLog:detail',
                          'admin:SysPermission:page', 'admin:SysPermission:grouped',
                          'admin:SysPermission:detail',
                          'admin:SysRole:page', 'admin:SysRole:detail', 'admin:SysRole:listMenus',
                          'admin:SysRole:listPermissions', 'admin:SysRole:pageUsers',
                          'admin:SysUser:page', 'admin:SysUser:detail', 'admin:SysUser:listRoles',
                          'admin:SysUserOauth:listByUser',
                          'catalog:Route:page', 'catalog:Route:detail',
                          'catalog:RouteCategory:tree', 'catalog:RouteCategory:detail',
                          'catalog:RouteItinerary:list', 'catalog:RouteImage:list',
                          'catalog:RouteTag:page', 'catalog:RouteTag:list',
                          'catalog:RouteTagRel:list',
                          'catalog:RouteDeparture:page', 'catalog:RouteDeparture:list',
                          'catalog:RouteReview:page', 'catalog:RouteReview:detail',
                          'trade:Orders:page', 'trade:Orders:detail',
                          'trade:OrderTraveler:listByOrder',
                          'trade:OrderStatusLog:page', 'trade:OrderStatusLog:listByOrder',
                          'trade:PaymentRecord:page', 'trade:PaymentRecord:listByOrder',
                          'trade:RefundRecord:page', 'trade:RefundRecord:detail',
                          'trade:RefundRecord:listByOrder',
                          'customer:Customer:page', 'customer:Customer:detail',
                          'customer:Customer:listCoupons',
                          'customer:CouponTemplate:page', 'customer:CouponTemplate:list',
                          'customer:CouponTemplate:detail',
                          'customer:Coupon:page', 'customer:Coupon:detail', 'customer:Coupon:list',
                          'operations:CsTicket:page', 'operations:CsTicket:detail',
                          'operations:CsTicket:statistics',
                          'operations:CsMessage:page', 'operations:CsMessage:listByTicket',
                          'operations:CustomRequirement:page', 'operations:CustomRequirement:detail',
                          'operations:CustomQuote:page', 'operations:CustomQuote:detail',
                          'operations:CustomQuote:listByRequirement',
                          'operations:StatDaily:page', 'operations:StatDaily:detail',
                          'operations:StatDaily:summary', 'operations:StatDaily:trend');

-- ---------------------------------------------------------------------------
-- 5. 角色 ↔ 权限点（覆盖式）
-- ---------------------------------------------------------------------------
DELETE FROM sys_role_permission WHERE role_id BETWEEN 1 AND 50;

-- 超级管理员：全部权限点（鉴权时还会直接命中通配符 *，此处授权仅用于界面展示）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE deleted = 0;

-- 系统管理员：除「权限点」与「菜单」的结构变更外的全部权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id
FROM sys_permission
WHERE deleted = 0
  AND permission_code NOT IN ('admin:SysPermission:create',
                              'admin:SysPermission:update',
                              'admin:SysPermission:delete',
                              'admin:SysPermission:updateStatus',
                              'admin:SysMenu:create',
                              'admin:SysMenu:update',
                              'admin:SysMenu:delete',
                              'admin:SysMenu:updateStatus',
                              'admin:SysMenu:updateVisible');

-- 客服
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id
FROM sys_permission
WHERE deleted = 0
  AND permission_code IN ('admin:Dashboard:overview',
                          'admin:SysUser:page', 'admin:SysUser:detail',
                          'admin:SysUserOauth:listByUser',
                          'admin:SysNotice:page', 'admin:SysNotice:detail',
                          'admin:SysFile:page', 'admin:SysFile:detail', 'admin:SysFile:download',
                          'catalog:Route:page', 'catalog:Route:detail',
                          'catalog:RouteItinerary:list', 'catalog:RouteImage:list',
                          'catalog:RouteTagRel:list',
                          'catalog:RouteDeparture:list',
                          'catalog:RouteReview:page', 'catalog:RouteReview:detail',
                          'trade:Orders:page', 'trade:Orders:detail',
                          'trade:Orders:create', 'trade:Orders:update',
                          'trade:Orders:cancel', 'trade:Orders:pay',
                          'trade:Orders:startTravel', 'trade:Orders:complete',
                          'trade:OrderTraveler:listByOrder', 'trade:OrderTraveler:create',
                          'trade:OrderTraveler:update', 'trade:OrderTraveler:delete',
                          'trade:OrderTraveler:replaceAll',
                          'trade:OrderStatusLog:page', 'trade:OrderStatusLog:listByOrder',
                          'trade:PaymentRecord:page', 'trade:PaymentRecord:listByOrder',
                          'trade:RefundRecord:page', 'trade:RefundRecord:detail',
                          'trade:RefundRecord:listByOrder', 'trade:RefundRecord:apply',
                          'customer:Customer:page', 'customer:Customer:detail',
                          'customer:Customer:update', 'customer:Customer:updatePhone',
                          'customer:Customer:listCoupons',
                          'customer:CouponTemplate:page', 'customer:CouponTemplate:list',
                          'customer:CouponTemplate:detail',
                          'customer:Coupon:page', 'customer:Coupon:detail', 'customer:Coupon:list',
                          'customer:Coupon:use', 'customer:Coupon:revoke',
                          'operations:CsTicket:page', 'operations:CsTicket:detail',
                          'operations:CsTicket:create', 'operations:CsTicket:update',
                          'operations:CsTicket:assign', 'operations:CsTicket:updatePriority',
                          'operations:CsTicket:updateStatus',
                          'operations:CsMessage:page', 'operations:CsMessage:listByTicket',
                          'operations:CsMessage:create',
                          'operations:CustomRequirement:page', 'operations:CustomRequirement:detail',
                          'operations:CustomRequirement:create', 'operations:CustomRequirement:update',
                          'operations:CustomRequirement:assign',
                          'operations:CustomRequirement:updateStatus',
                          'operations:CustomQuote:page', 'operations:CustomQuote:detail',
                          'operations:CustomQuote:listByRequirement', 'operations:CustomQuote:create',
                          'operations:CustomQuote:update', 'operations:CustomQuote:accept',
                          'operations:CustomQuote:invalidate');

-- 普通用户 / 会员用户：不分配权限点（App 端角色）

-- ---------------------------------------------------------------------------
-- 6. 内置超级管理员账号
--    账号：taishangxue520    密码：12345678Ss
--    手机：18807420597       邮箱：2120387318@qq.com
--    密码为 BCrypt(strength=10) 密文；账号可直接用「账号/手机号/邮箱」任一方式登录。
-- ---------------------------------------------------------------------------
INSERT INTO sys_user (username, password, nickname, phone, email, status, remark, version, deleted)
VALUES ('taishangxue520',
        '$2a$10$x654UBZnoATXH/jj8UhceOw7aY.X3hWhXjVNhpkGOx/vUXZxjK8SC',
        '太上雪', '18807420597', '2120387318@qq.com', 1,
        '内置超级管理员，首次登录后请立即修改密码', 0, 0)
ON DUPLICATE KEY UPDATE password = VALUES(password),
                        nickname = VALUES(nickname),
                        phone    = VALUES(phone),
                        email    = VALUES(email),
                        status   = 1,
                        deleted  = 0;

-- 重置该账号的角色绑定，只保留超级管理员
DELETE ur
FROM sys_user_role ur
         JOIN sys_user u ON u.id = ur.user_id
WHERE u.username = 'taishangxue520';

INSERT INTO sys_user_role (user_id, role_id)
SELECT id, 1 FROM sys_user WHERE username = 'taishangxue520';

-- ---------------------------------------------------------------------------
-- 7. 字典（类型编码规则：小写字母开头，仅小写字母/数字/下划线）
--    字典项按脚本负责的 id 区间整段重建，避免历史编码（如订单状态 10/20/30）残留
-- ---------------------------------------------------------------------------
DELETE FROM sys_dict_data WHERE id BETWEEN 1 AND 40;
DELETE FROM sys_dict_type WHERE id BETWEEN 1 AND 10;

INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark, deleted)
VALUES (1, '用户状态', 'sys_user_status', 1, '后台账号启用状态', 0),
       (2, '通用状态', 'sys_common_status', 1, '启用 / 停用', 0),
       (3, '是否', 'sys_yes_no', 1, '通用是/否标记', 0),
       (4, '性别', 'sys_gender', 1, '客户与用户性别', 0),
       (5, '公告类型', 'sys_notice_type', 1, '通知 / 公告', 0),
       (6, '线路状态', 'sys_route_status', 1, '旅游线路上下架状态', 0),
       (7, '订单状态', 'sys_order_status', 1, '订单主流程状态', 0),
       (8, '支付方式', 'sys_pay_type', 1, '订单支付渠道', 0),
       (9, '客户来源', 'sys_customer_source', 1, '客户获客渠道', 0),
       (10, '退款状态', 'sys_refund_status', 1, '退款单审核状态', 0)
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name), status = 1, remark = VALUES(remark),
                        deleted = 0;

INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort, css_class, is_default,
                           status, remark, deleted)
VALUES (1, 'sys_user_status', '正常', '1', 1, 'success', 1, 1, '可正常登录', 0),
       (2, 'sys_user_status', '禁用', '0', 2, 'danger', 0, 1, '禁止登录', 0),

       (3, 'sys_common_status', '启用', '1', 1, 'success', 1, 1, NULL, 0),
       (4, 'sys_common_status', '停用', '0', 2, 'info', 0, 1, NULL, 0),

       (5, 'sys_yes_no', '是', 'Y', 1, 'success', 1, 1, NULL, 0),
       (6, 'sys_yes_no', '否', 'N', 2, 'info', 0, 1, NULL, 0),

       (7, 'sys_gender', '未知', '0', 1, 'info', 1, 1, NULL, 0),
       (8, 'sys_gender', '男', '1', 2, 'primary', 0, 1, NULL, 0),
       (9, 'sys_gender', '女', '2', 3, 'danger', 0, 1, NULL, 0),

       (10, 'sys_notice_type', '通知', '1', 1, 'warning', 1, 1, '需要用户知晓的事项', 0),
       (11, 'sys_notice_type', '公告', '2', 2, 'primary', 0, 1, '对外公开发布的内容', 0),

       (12, 'sys_route_status', '已下架', '0', 1, 'info', 1, 1, '不对外售卖，可再次上架', 0),
       (13, 'sys_route_status', '已上架', '1', 2, 'success', 0, 1, '可被用户检索与下单', 0),

       -- 订单状态取值与 OrderStatus 枚举严格一致（0~6），不要改成 10/20 这类编码
       (15, 'sys_order_status', '待支付', '0', 1, 'warning', 1, 1, '下单未付款，超时可自动取消', 0),
       (16, 'sys_order_status', '已支付', '1', 2, 'primary', 0, 1, '已收款，等待出行', 0),
       (17, 'sys_order_status', '出行中', '2', 3, 'primary', 0, 1, '行程进行中', 0),
       (18, 'sys_order_status', '已完成', '3', 4, 'success', 0, 1, '行程结束，终态', 0),
       (19, 'sys_order_status', '已取消', '4', 5, 'info', 0, 1, '待支付阶段取消，库存已回补', 0),
       (20, 'sys_order_status', '退款中', '5', 6, 'warning', 0, 1, '退款申请待审核', 0),
       (34, 'sys_order_status', '已退款', '6', 7, 'danger', 0, 1, '全额退款完成，终态', 0),

       -- 支付方式取值与 TradeDict.payTypeDesc 一致
       (21, 'sys_pay_type', '模拟支付', '1', 1, 'info', 1, 1, '后台代录时人工确认收款', 0),
       (22, 'sys_pay_type', '微信支付', '2', 2, 'success', 0, 1, '小程序端在线支付', 0),

       (25, 'sys_customer_source', '官网', 'website', 1, 'primary', 1, 1, NULL, 0),
       (26, 'sys_customer_source', '小程序', 'miniapp', 2, 'success', 0, 1, NULL, 0),
       (27, 'sys_customer_source', '电话咨询', 'phone', 3, 'info', 0, 1, NULL, 0),
       (28, 'sys_customer_source', '线下门店', 'store', 4, 'warning', 0, 1, NULL, 0),
       (29, 'sys_customer_source', '合作渠道', 'partner', 5, 'info', 0, 1, NULL, 0),

       (30, 'sys_refund_status', '待审核', '0', 1, 'warning', 1, 1, NULL, 0),
       (31, 'sys_refund_status', '已通过', '1', 2, 'success', 0, 1, '等待打款', 0),
       (32, 'sys_refund_status', '已驳回', '2', 3, 'danger', 0, 1, NULL, 0),
       (33, 'sys_refund_status', '已退款', '3', 4, 'info', 0, 1, '款项已退回', 0)
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label), sort = VALUES(sort),
                        css_class  = VALUES(css_class), is_default = VALUES(is_default),
                        status     = VALUES(status), remark = VALUES(remark), deleted = 0;

-- ---------------------------------------------------------------------------
-- 8. 系统参数
-- ---------------------------------------------------------------------------
INSERT INTO sys_config (id, config_name, config_key, config_value, remark, deleted)
VALUES (1, '站点名称', 'site.name', 'RouteGo 旅游电商平台', '用于后台标题与对外展示', 0),
       (2, '备案号', 'site.icp', '京ICP备00000000号', '页脚展示，正式上线前替换为真实备案号', 0),
       (3, '客服电话', 'site.service.phone', '400-800-1234', '用户端与后台帮助入口展示', 0),
       (4, '客服邮箱', 'site.service.email', 'service@routego.cn', '投诉与商务合作接收邮箱', 0),
       (5, '默认分页条数', 'page.default.size', '10', '列表接口未显式传 pageSize 时的默认值', 0),
       (6, '上传大小上限(MB)', 'upload.max.size.mb', '20', '与 spring.servlet.multipart 配置保持一致', 0),
       (7, '订单自动取消(分钟)', 'order.auto.cancel.minutes', '30', '待支付订单超时自动取消', 0),
       (8, '密码最小长度', 'security.password.min.length', '8', '新增用户与重置密码时的校验下限', 0)
ON DUPLICATE KEY UPDATE config_name = VALUES(config_name), config_value = VALUES(config_value),
                        remark      = VALUES(remark), deleted = 0;

-- ---------------------------------------------------------------------------
-- 9. 公告（status：1 已发布，0 草稿/已下架）
-- ---------------------------------------------------------------------------
INSERT INTO sys_notice (id, title, content, type, status, is_top, publish_time, deleted)
VALUES (1, 'RouteGo 后台正式上线',
        'RouteGo 旅游电商平台后台已完成权限体系升级：统一账号登录（账号/手机号/邮箱）、短信验证码登录、第三方登录与 IP 归属地记录均已启用。如遇问题请联系系统管理员。',
        1, 1, 1, NOW(), 0),
       (2, '权限体系升级说明',
        '本次升级将权限码统一为 admin:<Controller>:<method> 格式，并为每个权限点补充了中文说明。角色授权抽屉支持按关键字、模块筛选，可勾选「仅看已选」快速核对；调整角色权限后，相关用户重新登录即可生效。',
        2, 1, 0, NOW(), 0),
       (3, '初始化数据使用指引',
        '内置账号 taishangxue520 为超级管理员，首次登录后请立即修改密码，并为其绑定手机号与邮箱以便找回。管理员可在「系统管理 - 角色管理」中按岗位创建角色，再为角色分配菜单与权限点。',
        1, 1, 0, NOW(), 0)
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content), type = VALUES(type),
                        status = VALUES(status), is_top = VALUES(is_top), deleted = 0;
