-- =============================================================================
-- 2026-11-05 角色精简 + 注册默认角色
-- =============================================================================
-- 背景：用户要求内置角色只保留 5 个——超级管理员、系统管理员、客服、普通用户、
-- 会员用户；且注册用户默认绑定「普通用户」。
--
-- 方案：复用 id 1-5 重命名（保持已有角色 ID 引用稳定），原 id 6/7/8
-- （客服专员/数据分析师/只读访客）物理删除；taishangxue520 的绑定从
-- 旧 super_admin(id=2) 迁到新 super_admin(id=1)。
-- 可重复执行：先清关联再重排，最后统一重建授权。
-- =============================================================================

-- 1. 解绑旧 super_admin(id=2) 上的用户，迁到新 super_admin(id=1)
DELETE ur FROM sys_user_role ur
         JOIN sys_user u ON u.id = ur.user_id
WHERE ur.role_id = 2 AND u.username = 'taishangxue520';
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT id, 1 FROM sys_user WHERE username = 'taishangxue520';

-- 2. 删除不再保留的角色（先清关联；这些角色未被任何用户绑定）
DELETE FROM sys_user_role WHERE role_id IN (6, 7, 8);
DELETE FROM sys_role_menu WHERE role_id IN (6, 7, 8);
DELETE FROM sys_role_permission WHERE role_id IN (6, 7, 8);
DELETE FROM sys_role WHERE id IN (6, 7, 8);

-- 3. 按 5 角色重排（id 1-5）
-- role_code 有全局唯一索引，先腾挪再改名，保证脚本可重复执行
UPDATE sys_role SET role_code = '__migrating_admin' WHERE id = 2;
UPDATE sys_role SET
  role_name = '超级管理员', role_code = 'super_admin', sort = 1, status = 1, deleted = 0,
  remark = '平台最高权限，鉴权时直接返回通配权限 *，拥有全部菜单，不可删除'
WHERE id = 1;
UPDATE sys_role SET
  role_name = '系统管理员', role_code = 'admin', sort = 10, status = 1, deleted = 0,
  remark = '维护账号、角色、字典、参数、公告、文件与日志；不含权限点与菜单结构的增删改'
WHERE id = 2;
UPDATE sys_role SET
  role_name = '客服', role_code = 'support', sort = 20, status = 1, deleted = 0,
  remark = '处理工单、定制需求与订单客诉，可查用户资料与线路信息'
WHERE id = 3;
UPDATE sys_role SET
  role_name = '普通用户', role_code = 'normal_user', sort = 90, status = 1, deleted = 0,
  remark = '注册用户默认角色（App 端），不分配后台菜单与权限点'
WHERE id = 4;
UPDATE sys_role SET
  role_name = '会员用户', role_code = 'member_user', sort = 91, status = 1, deleted = 0,
  remark = '付费会员角色，预留会员体系扩展'
WHERE id = 5;

-- 4. 重建角色 ↔ 菜单（普通用户 / 会员用户不分配后台菜单）
DELETE FROM sys_role_menu WHERE role_id BETWEEN 1 AND 5;

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

-- 5. 重建角色 ↔ 权限点
DELETE FROM sys_role_permission WHERE role_id BETWEEN 1 AND 5;

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

-- 普通用户 / 会员用户：不分配权限点（App 端角色，后台无可操作菜单）

-- 6. 清理权限缓存（角色授权变更后 Sa-Token 权限缓存需失效）
-- 应用启动后由缓存 TTL 自然过期，或手动清理：
--   redis-cli --scan --pattern 'admin:*' | xargs redis-cli del
