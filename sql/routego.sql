-- =============================================================================
-- 全国风景线路旅游电商平台 · 单体数据库初始化脚本
-- 版本：V2.0        日期：2026-11-04
-- 数据库：MySQL 8.0  字符集：utf8mb4  排序规则：utf8mb4_general_ci
-- 库名：routego（单体多模块，所有业务表合并到同一库）
-- 业务表：36 张 + 1 张 Seata AT undo_log
--
-- 说明：
--   1. 原微服务版的 5 个库（routego_admin/catalog/trade/customer/operations）
--      合并为单一 routego 库，表名之间无冲突。
--   2. 不建物理外键，跨业务域关系由应用层保证。
--   3. 业务表统一包含审计字段（create_by/create_time/update_by/update_time）
--      与逻辑删除字段 deleted。
--   4. 保留一张 Seata AT undo_log 基础表，单体下如需 Seata 可直接使用。
-- =============================================================================

CREATE DATABASE IF NOT EXISTS routego
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE routego;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS undo_log (
  branch_id     bigint       NOT NULL COMMENT '分支事务 ID',
  xid           varchar(128) NOT NULL COMMENT '全局事务 ID',
  context       varchar(128) NOT NULL COMMENT '回滚上下文',
  rollback_info longblob     NOT NULL COMMENT '回滚信息',
  log_status    int          NOT NULL COMMENT '日志状态：0 正常，1 防御',
  log_created   datetime(6)  NOT NULL COMMENT '创建时间',
  log_modified  datetime(6)  NOT NULL COMMENT '修改时间',
  ext           varchar(100) DEFAULT NULL COMMENT '保留字段',
  UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Seata AT undo log';

-- =============================================================================
-- 一、系统与权限域（15 张）
-- =============================================================================

-- 1. 系统用户表（管理员、运营、订单、客服、财务等后台账号）
CREATE TABLE IF NOT EXISTS sys_user (
  id            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username      varchar(50)  NOT NULL COMMENT '登录账号',
  password      varchar(100) NOT NULL COMMENT '密码（BCrypt 密文）',
  nickname      varchar(50)  DEFAULT NULL COMMENT '昵称',
  avatar        varchar(255) DEFAULT NULL COMMENT '头像 URL',
  phone         varchar(128) DEFAULT NULL COMMENT '手机号（AES 密文）',
  phone_index   varchar(64)  DEFAULT NULL COMMENT '手机号盲索引（HMAC-SHA256）',
  email         varchar(100) DEFAULT NULL COMMENT '邮箱',
  status        tinyint      NOT NULL DEFAULT 1 COMMENT '状态：0 禁用，1 启用',
  last_login_time datetime   DEFAULT NULL COMMENT '最后登录时间',
  last_login_ip varchar(50)  DEFAULT NULL COMMENT '最后登录 IP',
  remark        varchar(255) DEFAULT NULL COMMENT '备注',
  version       int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  create_by     bigint       DEFAULT NULL COMMENT '创建人 ID',
  create_time   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by     bigint       DEFAULT NULL COMMENT '更新人 ID',
  update_time   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_phone_index (phone_index),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户表';

-- 2. 角色表
CREATE TABLE IF NOT EXISTS sys_role (
  id          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_name   varchar(50)  NOT NULL COMMENT '角色名称',
  role_code   varchar(50)  NOT NULL COMMENT '角色编码',
  sort        int          NOT NULL DEFAULT 0 COMMENT '排序',
  status      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：0 禁用，1 启用',
  remark      varchar(255) DEFAULT NULL COMMENT '备注',
  version     int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  create_by   bigint       DEFAULT NULL COMMENT '创建人 ID',
  create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by   bigint       DEFAULT NULL COMMENT '更新人 ID',
  update_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- 3. 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
  id          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     bigint   NOT NULL COMMENT '用户 ID',
  role_id     bigint   NOT NULL COMMENT '角色 ID',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

-- 4. 菜单权限表
CREATE TABLE IF NOT EXISTS sys_menu (
  id          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id   bigint       NOT NULL DEFAULT 0 COMMENT '父级 ID，顶级为 0',
  menu_name   varchar(50)  NOT NULL COMMENT '菜单名称',
  menu_type   char(1)      NOT NULL COMMENT '类型：M 目录，C 菜单，F 按钮',
  path        varchar(200) DEFAULT NULL COMMENT '前端路由地址',
  component   varchar(200) DEFAULT NULL COMMENT '前端组件路径',
  perms       varchar(100) DEFAULT NULL COMMENT '权限标识，如 route:add',
  icon        varchar(100) DEFAULT NULL COMMENT '图标',
  sort        int          NOT NULL DEFAULT 0 COMMENT '排序',
  visible     tinyint      NOT NULL DEFAULT 1 COMMENT '是否显示：0 隐藏，1 显示',
  status      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：0 禁用，1 启用',
  version     int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  create_by   bigint       DEFAULT NULL COMMENT '创建人 ID',
  create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by   bigint       DEFAULT NULL COMMENT '更新人 ID',
  update_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  KEY idx_parent_id (parent_id),
  KEY idx_perms (perms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单表';

-- 5. 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
  id          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_id     bigint   NOT NULL COMMENT '角色 ID',
  menu_id     bigint   NOT NULL COMMENT '菜单 ID',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_menu (role_id, menu_id),
  KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联表';

-- 6. 权限点表
CREATE TABLE IF NOT EXISTS sys_permission (
  id              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  permission_name varchar(50)  NOT NULL COMMENT '权限名称',
  permission_code varchar(100) NOT NULL COMMENT '权限码，如 route:add',
  module          varchar(50)  DEFAULT NULL COMMENT '所属模块',
  description     varchar(255) DEFAULT NULL COMMENT '说明',
  status          tinyint      NOT NULL DEFAULT 1 COMMENT '状态：0 禁用，1 启用',
  create_by       bigint       DEFAULT NULL COMMENT '创建人 ID',
  create_time     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by       bigint       DEFAULT NULL COMMENT '更新人 ID',
  update_time     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted         tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_permission_code (permission_code),
  KEY idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='权限点表';

-- 7. 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
  id            bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_id       bigint   NOT NULL COMMENT '角色 ID',
  permission_id bigint   NOT NULL COMMENT '权限点 ID',
  create_time   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_permission (role_id, permission_id),
  KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色权限关联表';

-- 8. 后台用户第三方登录绑定表
CREATE TABLE IF NOT EXISTS sys_user_oauth (
  id          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     bigint       NOT NULL COMMENT '用户 ID',
  provider    varchar(20)  NOT NULL COMMENT '平台：wechat、qq、apple、dingtalk',
  openid      varchar(128) NOT NULL COMMENT '平台用户唯一标识',
  unionid     varchar(128) DEFAULT NULL COMMENT '开放平台统一标识',
  nickname    varchar(100) DEFAULT NULL COMMENT '平台昵称',
  avatar      varchar(255) DEFAULT NULL COMMENT '平台头像',
  bind_time   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_provider_openid (provider, openid),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='后台用户第三方登录绑定表';

-- 9. 字典类型表
CREATE TABLE IF NOT EXISTS sys_dict_type (
  id          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  dict_name   varchar(50)  NOT NULL COMMENT '字典名称',
  dict_type   varchar(50)  NOT NULL COMMENT '字典类型编码',
  status      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：0 禁用，1 启用',
  remark      varchar(255) DEFAULT NULL COMMENT '备注',
  create_by   bigint       DEFAULT NULL COMMENT '创建人 ID',
  create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by   bigint       DEFAULT NULL COMMENT '更新人 ID',
  update_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典类型表';

-- 10. 字典数据表
CREATE TABLE IF NOT EXISTS sys_dict_data (
  id          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  dict_type   varchar(50)  NOT NULL COMMENT '字典类型编码',
  dict_label  varchar(100) NOT NULL COMMENT '字典标签',
  dict_value  varchar(50)  NOT NULL COMMENT '字典值',
  sort        int          NOT NULL DEFAULT 0 COMMENT '排序',
  css_class   varchar(100) DEFAULT NULL COMMENT '样式类名',
  is_default  tinyint      NOT NULL DEFAULT 0 COMMENT '是否默认：0 否，1 是',
  status      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：0 禁用，1 启用',
  remark      varchar(255) DEFAULT NULL COMMENT '备注',
  create_by   bigint       DEFAULT NULL COMMENT '创建人 ID',
  create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by   bigint       DEFAULT NULL COMMENT '更新人 ID',
  update_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_type_value (dict_type, dict_value),
  KEY idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典数据表';

-- 11. 系统参数表
CREATE TABLE IF NOT EXISTS sys_config (
  id           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  config_name  varchar(100) NOT NULL COMMENT '参数名称',
  config_key   varchar(100) NOT NULL COMMENT '参数键',
  config_value text         DEFAULT NULL COMMENT '参数值',
  remark       varchar(255) DEFAULT NULL COMMENT '备注',
  create_by    bigint       DEFAULT NULL COMMENT '创建人 ID',
  create_time  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by    bigint       DEFAULT NULL COMMENT '更新人 ID',
  update_time  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统参数表';

-- 12. 操作日志表（追加型）
CREATE TABLE IF NOT EXISTS sys_oper_log (
  id             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  title          varchar(100) NOT NULL COMMENT '操作模块',
  business_type  tinyint      NOT NULL DEFAULT 0 COMMENT '业务类型：1 新增，2 修改，3 删除，4 查询，5 导出，6 其他',
  method         varchar(200) DEFAULT NULL COMMENT '方法全限定名',
  request_method varchar(10)  DEFAULT NULL COMMENT '请求方法',
  oper_name      varchar(50)  DEFAULT NULL COMMENT '操作人',
  oper_url       varchar(255) DEFAULT NULL COMMENT '请求 URL',
  oper_ip        varchar(50)  DEFAULT NULL COMMENT '操作 IP',
  oper_param     mediumtext   DEFAULT NULL COMMENT '请求参数（脱敏、截断）',
  json_result    mediumtext   DEFAULT NULL COMMENT '返回结果（截断）',
  status         tinyint      NOT NULL DEFAULT 1 COMMENT '状态：0 失败，1 成功',
  error_msg      text         DEFAULT NULL COMMENT '异常信息',
  cost_time      bigint       DEFAULT NULL COMMENT '耗时（毫秒）',
  oper_time      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (id),
  KEY idx_oper_time (oper_time),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表';

-- 13. 登录日志表（追加型）
CREATE TABLE IF NOT EXISTS sys_login_log (
  id         bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  username   varchar(50) DEFAULT NULL COMMENT '登录账号',
  ip         varchar(50) DEFAULT NULL COMMENT '登录 IP',
  location   varchar(100) DEFAULT NULL COMMENT 'IP 归属地（省 市 · 运营商）',
  browser    varchar(100) DEFAULT NULL COMMENT '浏览器',
  os         varchar(100) DEFAULT NULL COMMENT '操作系统',
  status     tinyint     NOT NULL DEFAULT 1 COMMENT '状态：0 失败，1 成功',
  msg        varchar(255) DEFAULT NULL COMMENT '提示信息',
  login_time datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (id),
  KEY idx_username (username),
  KEY idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录日志表';

-- 14. 文件记录表
CREATE TABLE IF NOT EXISTS sys_file (
  id            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  file_name     varchar(255) NOT NULL COMMENT '存储文件名',
  original_name varchar(255) DEFAULT NULL COMMENT '原始文件名',
  file_path     varchar(500) NOT NULL COMMENT '存储路径',
  file_url      varchar(500) DEFAULT NULL COMMENT '访问 URL',
  file_type     varchar(50)  DEFAULT NULL COMMENT '文件类型',
  file_size     bigint       DEFAULT NULL COMMENT '文件大小（字节）',
  storage_type  tinyint      NOT NULL DEFAULT 1 COMMENT '存储类型：1 本地，2 OSS，3 MinIO',
  create_by     bigint       DEFAULT NULL COMMENT '创建人 ID',
  create_time   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by     bigint       DEFAULT NULL COMMENT '更新人 ID',
  update_time   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件记录表';

-- 15. 公告表
CREATE TABLE IF NOT EXISTS sys_notice (
  id           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  title        varchar(100) NOT NULL COMMENT '标题',
  content      text         DEFAULT NULL COMMENT '内容',
  type         tinyint      NOT NULL DEFAULT 1 COMMENT '类型：1 通知，2 公告',
  status       tinyint      NOT NULL DEFAULT 1 COMMENT '状态：0 下架，1 发布',
  is_top       tinyint      NOT NULL DEFAULT 0 COMMENT '是否置顶：0 否，1 是',
  publish_time datetime     DEFAULT NULL COMMENT '发布时间',
  create_by    bigint       DEFAULT NULL COMMENT '创建人 ID',
  create_time  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by    bigint       DEFAULT NULL COMMENT '更新人 ID',
  update_time  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公告表';

-- =============================================================================
-- RouteGo-catalog：线路商品、行程、班期与评价

-- Seata AT 分支事务回滚日志（基础表，不计入 36 张业务表）


-- 二、线路域（8 张）
-- =============================================================================

-- 16. 线路分类表
CREATE TABLE IF NOT EXISTS route_category (
  id          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id   bigint       NOT NULL DEFAULT 0 COMMENT '父级 ID，顶级为 0',
  name        varchar(50)  NOT NULL COMMENT '分类名称',
  sort        int          NOT NULL DEFAULT 0 COMMENT '排序',
  status      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：0 禁用，1 启用',
  icon        varchar(255) DEFAULT NULL COMMENT '图标',
  create_by   bigint       DEFAULT NULL COMMENT '创建人 ID',
  create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by   bigint       DEFAULT NULL COMMENT '更新人 ID',
  update_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='线路分类表';

-- 17. 线路主表
CREATE TABLE IF NOT EXISTS route (
  id              bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  category_id     bigint        NOT NULL COMMENT '分类 ID',
  route_no        varchar(32)   NOT NULL COMMENT '线路编号',
  title           varchar(100)  NOT NULL COMMENT '线路标题',
  subtitle        varchar(200)  DEFAULT NULL COMMENT '副标题/卖点',
  cover_image     varchar(255)  DEFAULT NULL COMMENT '封面图',
  destination     varchar(100)  NOT NULL COMMENT '目的地',
  departure_city  varchar(50)   NOT NULL COMMENT '出发城市',
  longitude       decimal(10,6) DEFAULT NULL COMMENT '目的地经度（GCJ-02）',
  latitude        decimal(10,6) DEFAULT NULL COMMENT '目的地纬度（GCJ-02）',
  address         varchar(255)  DEFAULT NULL COMMENT '目的地地址',
  days            int           NOT NULL COMMENT '行程天数',
  nights          int           NOT NULL DEFAULT 0 COMMENT '住宿晚数',
  min_price       decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '起价',
  original_price  decimal(10,2) DEFAULT NULL COMMENT '原价',
  highlights      text          DEFAULT NULL COMMENT '线路亮点',
  include_items   text          DEFAULT NULL COMMENT '费用包含',
  exclude_items   text          DEFAULT NULL COMMENT '费用不含',
  booking_notice  text          DEFAULT NULL COMMENT '预订须知',
  sales           int           NOT NULL DEFAULT 0 COMMENT '销量',
  rating          decimal(3,2)  NOT NULL DEFAULT 5.00 COMMENT '平均评分',
  status          tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 下架，1 上架',
  sort            int           NOT NULL DEFAULT 0 COMMENT '排序',
  version         int           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  create_by       bigint        DEFAULT NULL COMMENT '创建人 ID',
  create_time     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by       bigint        DEFAULT NULL COMMENT '更新人 ID',
  update_time     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted         tinyint       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_route_no (route_no),
  KEY idx_category_id (category_id),
  KEY idx_status (status),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='线路主表';

-- 18. 行程明细表
CREATE TABLE IF NOT EXISTS route_itinerary (
  id          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  route_id    bigint       NOT NULL COMMENT '线路 ID',
  day_no      int          NOT NULL COMMENT '第几天',
  title       varchar(100) NOT NULL COMMENT '当日主题',
  description text         DEFAULT NULL COMMENT '行程描述',
  meals       varchar(50)  DEFAULT NULL COMMENT '用餐说明',
  hotel       varchar(100) DEFAULT NULL COMMENT '住宿说明',
  transport   varchar(50)  DEFAULT NULL COMMENT '交通方式',
  image       varchar(255) DEFAULT NULL COMMENT '配图',
  longitude   decimal(10,6) DEFAULT NULL COMMENT '当日地点经度（GCJ-02）',
  latitude    decimal(10,6) DEFAULT NULL COMMENT '当日地点纬度（GCJ-02）',
  address     varchar(255) DEFAULT NULL COMMENT '当日地点地址',
  sort        int          NOT NULL DEFAULT 0 COMMENT '排序',
  create_by   bigint       DEFAULT NULL COMMENT '创建人 ID',
  create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by   bigint       DEFAULT NULL COMMENT '更新人 ID',
  update_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_route_day (route_id, day_no),
  KEY idx_route_id (route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='行程明细表';

-- 19. 线路图片表
CREATE TABLE IF NOT EXISTS route_image (
  id          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  route_id    bigint       NOT NULL COMMENT '线路 ID',
  image_url   varchar(500) NOT NULL COMMENT '图片 URL',
  sort        int          NOT NULL DEFAULT 0 COMMENT '排序',
  create_by   bigint       DEFAULT NULL COMMENT '创建人 ID',
  create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by   bigint       DEFAULT NULL COMMENT '更新人 ID',
  update_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  KEY idx_route_id (route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='线路图片表';

-- 20. 标签表
CREATE TABLE IF NOT EXISTS route_tag (
  id          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  tag_name    varchar(50)  NOT NULL COMMENT '标签名称',
  color       varchar(20)  DEFAULT NULL COMMENT '标签颜色',
  sort        int          NOT NULL DEFAULT 0 COMMENT '排序',
  status      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：0 禁用，1 启用',
  create_by   bigint       DEFAULT NULL COMMENT '创建人 ID',
  create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by   bigint       DEFAULT NULL COMMENT '更新人 ID',
  update_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='标签表';

-- 21. 线路标签关联表
CREATE TABLE IF NOT EXISTS route_tag_rel (
  id          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
  route_id    bigint   NOT NULL COMMENT '线路 ID',
  tag_id      bigint   NOT NULL COMMENT '标签 ID',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_route_tag (route_id, tag_id),
  KEY idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='线路标签关联表';

-- 22. 出发班期表
CREATE TABLE IF NOT EXISTS route_departure (
  id           bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  route_id     bigint        NOT NULL COMMENT '线路 ID',
  depart_date  date          NOT NULL COMMENT '出发日期',
  adult_price  decimal(10,2) NOT NULL COMMENT '成人价',
  child_price  decimal(10,2) DEFAULT NULL COMMENT '儿童价',
  stock        int           NOT NULL DEFAULT 0 COMMENT '库存',
  sold         int           NOT NULL DEFAULT 0 COMMENT '已售数量',
  status       tinyint       NOT NULL DEFAULT 1 COMMENT '状态：0 停售，1 可售',
  version      int           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  create_by    bigint        DEFAULT NULL COMMENT '创建人 ID',
  create_time  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by    bigint        DEFAULT NULL COMMENT '更新人 ID',
  update_time  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      tinyint       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_route_date (route_id, depart_date),
  KEY idx_depart_date (depart_date),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='出发班期表';

-- 23. 线路评价表
CREATE TABLE IF NOT EXISTS route_review (
  id          bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  route_id    bigint        NOT NULL COMMENT '线路 ID',
  order_id    bigint        NOT NULL COMMENT '订单 ID',
  customer_id bigint        NOT NULL COMMENT '客户 ID',
  rating      tinyint       NOT NULL COMMENT '评分 1-5',
  content     text          DEFAULT NULL COMMENT '评价内容',
  images      text          DEFAULT NULL COMMENT '评价图片（JSON 数组）',
  reply       text          DEFAULT NULL COMMENT '后台回复',
  reply_time  datetime      DEFAULT NULL COMMENT '回复时间',
  status      tinyint       NOT NULL DEFAULT 1 COMMENT '状态：0 隐藏，1 显示',
  version     int           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  create_time datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     tinyint       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_id (order_id),
  KEY idx_route_id (route_id),
  KEY idx_customer_id (customer_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='线路评价表';

-- =============================================================================
-- RouteGo-trade：订单、支付与退款

-- Seata AT 分支事务回滚日志（基础表，不计入 36 张业务表）


-- 三、订单域（5 张）
-- =============================================================================

-- 24. 订单主表
CREATE TABLE IF NOT EXISTS orders (
  id                  bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_no            varchar(32)   NOT NULL COMMENT '订单号',
  customer_id         bigint        NOT NULL COMMENT '客户 ID',
  route_id            bigint        NOT NULL COMMENT '线路 ID',
  departure_id        bigint        DEFAULT NULL COMMENT '班期 ID',
  route_title         varchar(100)  NOT NULL COMMENT '线路标题快照',
  depart_date         date          DEFAULT NULL COMMENT '出发日期快照',
  traveler_count      int           NOT NULL DEFAULT 1 COMMENT '出行人数',
  unit_price          decimal(10,2) NOT NULL COMMENT '单价快照',
  total_amount        decimal(10,2) NOT NULL COMMENT '订单总额',
  discount_amount     decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  pay_amount          decimal(10,2) NOT NULL COMMENT '实付金额',
  contact_name        varchar(50)   NOT NULL COMMENT '联系人',
  contact_phone       varchar(128)  NOT NULL COMMENT '联系电话（AES 密文）',
  contact_phone_index varchar(64)   DEFAULT NULL COMMENT '联系电话盲索引（HMAC）',
  status              tinyint       NOT NULL DEFAULT 0 COMMENT '订单状态：0 待支付，1 已支付，2 出行中，3 已完成，4 已取消，5 退款中，6 已退款',
  pay_status          tinyint       NOT NULL DEFAULT 0 COMMENT '支付状态：0 未支付，1 已支付，2 已退款',
  pay_time            datetime      DEFAULT NULL COMMENT '支付时间',
  cancel_time         datetime      DEFAULT NULL COMMENT '取消时间',
  cancel_reason       varchar(255)  DEFAULT NULL COMMENT '取消原因',
  remark              varchar(255)  DEFAULT NULL COMMENT '备注',
  source              tinyint       NOT NULL DEFAULT 2 COMMENT '来源：1 后台代录，2 微信小程序',
  version             int           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  create_by           bigint        DEFAULT NULL COMMENT '创建人 ID',
  create_time         datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by           bigint        DEFAULT NULL COMMENT '更新人 ID',
  update_time         datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted             tinyint       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_customer_id (customer_id),
  KEY idx_route_id (route_id),
  KEY idx_departure_id (departure_id),
  KEY idx_status (status),
  KEY idx_contact_phone_index (contact_phone_index),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单主表';

-- 25. 出行人表
CREATE TABLE IF NOT EXISTS order_traveler (
  id            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id      bigint       NOT NULL COMMENT '订单 ID',
  name          varchar(50)  NOT NULL COMMENT '姓名',
  id_card       varchar(255) DEFAULT NULL COMMENT '证件号（AES 密文）',
  phone         varchar(128) DEFAULT NULL COMMENT '手机号（AES 密文）',
  gender        tinyint      DEFAULT NULL COMMENT '性别：0 未知，1 男，2 女',
  traveler_type tinyint      NOT NULL DEFAULT 1 COMMENT '类型：1 成人，2 儿童',
  remark        varchar(255) DEFAULT NULL COMMENT '备注',
  create_time   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='出行人表';

-- 26. 订单状态流水表（追加型）
CREATE TABLE IF NOT EXISTS order_status_log (
  id            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id      bigint       NOT NULL COMMENT '订单 ID',
  from_status   tinyint      DEFAULT NULL COMMENT '原状态',
  to_status     tinyint      NOT NULL COMMENT '新状态',
  operator_id   bigint       DEFAULT NULL COMMENT '操作人 ID',
  operator_name varchar(50)  DEFAULT NULL COMMENT '操作人名称',
  remark        varchar(255) DEFAULT NULL COMMENT '备注',
  create_time   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_order_id (order_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单状态流水表';

-- 27. 支付记录表（追加型）
CREATE TABLE IF NOT EXISTS payment_record (
  id             bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id       bigint        NOT NULL COMMENT '订单 ID',
  pay_no         varchar(64)   NOT NULL COMMENT '支付单号',
  pay_type       tinyint       NOT NULL DEFAULT 1 COMMENT '支付方式：1 模拟，2 微信支付',
  amount         decimal(10,2) NOT NULL COMMENT '支付金额',
  status         tinyint       NOT NULL DEFAULT 1 COMMENT '状态：0 失败，1 成功',
  pay_time       datetime      DEFAULT NULL COMMENT '支付时间',
  transaction_id varchar(100)  DEFAULT NULL COMMENT '第三方交易号',
  create_time    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_pay_no (pay_no),
  KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付记录表';

-- 28. 退款记录表
CREATE TABLE IF NOT EXISTS refund_record (
  id           bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id     bigint        NOT NULL COMMENT '订单 ID',
  refund_no    varchar(64)   NOT NULL COMMENT '退款单号',
  amount       decimal(10,2) NOT NULL COMMENT '退款金额',
  reason       varchar(255)  DEFAULT NULL COMMENT '退款原因',
  status       tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 待审核，1 已通过，2 已驳回，3 已退款',
  apply_time   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  audit_time   datetime      DEFAULT NULL COMMENT '审核时间',
  audit_by     bigint        DEFAULT NULL COMMENT '审核人 ID',
  audit_remark varchar(255)  DEFAULT NULL COMMENT '审核意见',
  version      int           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  create_time  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted      tinyint       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_refund_no (refund_no),
  KEY idx_order_id (order_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='退款记录表';

-- =============================================================================
-- RouteGo-customer：会员、优惠券与 C 端身份

-- Seata AT 分支事务回滚日志（基础表，不计入 36 张业务表）


-- 四、客户与营销域（3 张）
-- =============================================================================

-- 29. 会员表（C 端，微信小程序用户）
CREATE TABLE IF NOT EXISTS customer (
  id              bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  phone           varchar(128)  NOT NULL COMMENT '手机号（AES 密文）',
  phone_index     varchar(64)   NOT NULL COMMENT '手机号盲索引（HMAC-SHA256）',
  openid          varchar(64)   DEFAULT NULL COMMENT '微信小程序 openid',
  unionid         varchar(64)   DEFAULT NULL COMMENT '微信开放平台 unionid',
  password        varchar(100)  DEFAULT NULL COMMENT '密码（BCrypt，可空）',
  nickname        varchar(50)   DEFAULT NULL COMMENT '昵称',
  avatar          varchar(255)  DEFAULT NULL COMMENT '头像',
  real_name       varchar(50)   DEFAULT NULL COMMENT '真实姓名',
  id_card         varchar(255)  DEFAULT NULL COMMENT '证件号（AES 密文）',
  gender          tinyint       DEFAULT NULL COMMENT '性别：0 未知，1 男，2 女',
  birthday        date          DEFAULT NULL COMMENT '生日',
  email           varchar(100)  DEFAULT NULL COMMENT '邮箱',
  level           tinyint       NOT NULL DEFAULT 1 COMMENT '会员等级',
  points          int           NOT NULL DEFAULT 0 COMMENT '积分',
  order_count     int           NOT NULL DEFAULT 0 COMMENT '累计订单数',
  total_amount    decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费金额',
  status          tinyint       NOT NULL DEFAULT 1 COMMENT '状态：0 禁用，1 正常',
  register_time   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  last_login_time datetime      DEFAULT NULL COMMENT '最后登录时间',
  create_time     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted         tinyint       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_phone_index (phone_index),
  UNIQUE KEY uk_openid (openid),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会员表';

-- 30. 优惠券模板表
CREATE TABLE IF NOT EXISTS coupon_template (
  id             bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  name           varchar(100)  NOT NULL COMMENT '优惠券名称',
  type           tinyint       NOT NULL COMMENT '类型：1 满减，2 折扣',
  threshold      decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛',
  amount         decimal(10,2) DEFAULT NULL COMMENT '减免金额',
  discount       decimal(5,2)  DEFAULT NULL COMMENT '折扣率（如 0.90）',
  total_count    int           NOT NULL DEFAULT 0 COMMENT '发行总量',
  received_count int           NOT NULL DEFAULT 0 COMMENT '已领取数量',
  valid_type     tinyint       NOT NULL DEFAULT 1 COMMENT '有效期类型：1 固定区间，2 领取后 N 天',
  start_time     datetime      DEFAULT NULL COMMENT '有效期开始',
  end_time       datetime      DEFAULT NULL COMMENT '有效期结束',
  valid_days     int           DEFAULT NULL COMMENT '领取后有效天数',
  status         tinyint       NOT NULL DEFAULT 1 COMMENT '状态：0 停用，1 启用',
  version        int           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  create_by      bigint        DEFAULT NULL COMMENT '创建人 ID',
  create_time    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by      bigint        DEFAULT NULL COMMENT '更新人 ID',
  update_time    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted        tinyint       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='优惠券模板表';

-- 31. 用户优惠券表
CREATE TABLE IF NOT EXISTS coupon (
  id          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  template_id bigint      NOT NULL COMMENT '模板 ID',
  customer_id bigint      NOT NULL COMMENT '客户 ID',
  code        varchar(64) NOT NULL COMMENT '优惠券码',
  status      tinyint     NOT NULL DEFAULT 0 COMMENT '状态：0 未使用，1 已使用，2 已过期',
  receive_time datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  use_time    datetime    DEFAULT NULL COMMENT '使用时间',
  order_id    bigint      DEFAULT NULL COMMENT '使用的订单 ID',
  version     int         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  create_time datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     tinyint     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code),
  KEY idx_customer_id (customer_id),
  KEY idx_template_id (template_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户优惠券表';

-- =============================================================================
-- RouteGo-operations：客服、定制游与经营统计

-- Seata AT 分支事务回滚日志（基础表，不计入 36 张业务表）


-- 五、客服域（2 张）
-- =============================================================================

-- 32. 客服工单表
CREATE TABLE IF NOT EXISTS cs_ticket (
  id          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  ticket_no   varchar(32)  NOT NULL COMMENT '工单号',
  customer_id bigint       DEFAULT NULL COMMENT '客户 ID',
  order_id    bigint       DEFAULT NULL COMMENT '关联订单 ID',
  type        tinyint      NOT NULL DEFAULT 1 COMMENT '类型：1 售前，2 订单，3 退款，4 投诉，5 其他',
  title       varchar(100) NOT NULL COMMENT '工单标题',
  content     text         NOT NULL COMMENT '问题描述',
  priority    tinyint      NOT NULL DEFAULT 2 COMMENT '优先级：1 低，2 中，3 高',
  status      tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0 待处理，1 处理中，2 已解决，3 已关闭',
  handler_id  bigint       DEFAULT NULL COMMENT '处理人 ID',
  finish_time datetime     DEFAULT NULL COMMENT '完成时间',
  last_message_time datetime DEFAULT NULL COMMENT '最后一条消息时间，会话列表按它排序',
  last_message varchar(200) DEFAULT NULL COMMENT '最后一条消息摘要，会话列表直接展示',
  customer_unread int      NOT NULL DEFAULT 0 COMMENT '客户未读数：客服回复后 +1，客户打开会话清零',
  staff_unread int         NOT NULL DEFAULT 0 COMMENT '客服未读数：客户发消息后 +1，客服打开会话清零',
  version     int          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  create_by   bigint       DEFAULT NULL COMMENT '创建人 ID',
  create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by   bigint       DEFAULT NULL COMMENT '更新人 ID',
  update_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ticket_no (ticket_no),
  KEY idx_customer_id (customer_id),
  KEY idx_order_id (order_id),
  KEY idx_status (status),
  KEY idx_handler_id (handler_id),
  KEY idx_last_message_time (last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客服工单表';

-- 33. 工单消息表（追加型）
CREATE TABLE IF NOT EXISTS cs_message (
  id          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
  ticket_id   bigint   NOT NULL COMMENT '工单 ID',
  sender_type tinyint  NOT NULL COMMENT '发送方：1 客户，2 系统用户',
  sender_id   bigint   NOT NULL COMMENT '发送方 ID',
  content     text     NOT NULL COMMENT '消息内容',
  images      text     DEFAULT NULL COMMENT '图片（JSON 数组）',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_ticket_id (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单消息表';

-- =============================================================================
-- 六、定制游域（2 张）
-- =============================================================================

-- 34. 定制需求表
CREATE TABLE IF NOT EXISTS custom_requirement (
  id            bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  requirement_no varchar(32)  NOT NULL COMMENT '需求单号',
  customer_id   bigint        DEFAULT NULL COMMENT '客户 ID',
  contact_name  varchar(50)   NOT NULL COMMENT '联系人',
  contact_phone varchar(128)  NOT NULL COMMENT '联系电话（AES 密文）',
  destination   varchar(100)  NOT NULL COMMENT '目的地',
  depart_date   date          DEFAULT NULL COMMENT '期望出发日期',
  days          int           DEFAULT NULL COMMENT '天数',
  people_count  int           DEFAULT NULL COMMENT '人数',
  budget        decimal(10,2) DEFAULT NULL COMMENT '预算',
  preferences   text          DEFAULT NULL COMMENT '偏好说明',
  status        tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 待跟进，1 已报价，2 已成交，3 已关闭',
  handler_id    bigint        DEFAULT NULL COMMENT '跟进人 ID',
  remark        varchar(255)  DEFAULT NULL COMMENT '备注',
  version       int           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  create_by     bigint        DEFAULT NULL COMMENT '创建人 ID',
  create_time   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by     bigint        DEFAULT NULL COMMENT '更新人 ID',
  update_time   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       tinyint       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_requirement_no (requirement_no),
  KEY idx_customer_id (customer_id),
  KEY idx_status (status),
  KEY idx_handler_id (handler_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='定制需求表';

-- 35. 定制报价表（追加型）
CREATE TABLE IF NOT EXISTS custom_quote (
  id             bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  requirement_id bigint        NOT NULL COMMENT '需求单 ID',
  quote_no       varchar(32)   NOT NULL COMMENT '报价单号',
  amount         decimal(10,2) NOT NULL COMMENT '报价金额',
  detail         text          DEFAULT NULL COMMENT '报价明细',
  valid_until    date          DEFAULT NULL COMMENT '有效期至',
  status         tinyint       NOT NULL DEFAULT 0 COMMENT '状态：0 待确认，1 已采纳，2 已失效',
  create_by      bigint        DEFAULT NULL COMMENT '创建人 ID',
  create_time    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_quote_no (quote_no),
  KEY idx_requirement_id (requirement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='定制报价表';

-- =============================================================================
-- 七、统计域（1 张）
-- =============================================================================

-- 36. 每日经营统计表
CREATE TABLE IF NOT EXISTS stat_daily (
  id                 bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  stat_date          date          NOT NULL COMMENT '统计日期',
  order_count        int           NOT NULL DEFAULT 0 COMMENT '订单数',
  paid_order_count   int           NOT NULL DEFAULT 0 COMMENT '已支付订单数',
  sales_amount       decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '销售额',
  refund_amount      decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
  new_customer_count int           NOT NULL DEFAULT 0 COMMENT '新增客户数',
  create_time        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每日经营统计表';

-- =============================================================================
-- 结构脚本结束：1 个数据库 routego、36 张业务表、1 张 undo_log 基础表
-- 初始化数据（管理员、角色、菜单、字典）见方案书 7.5，可整理为 data.sql
-- =============================================================================
