const ROLE_DEFS = [
  {
    id: 1,
    roleName: '系统管理员',
    roleCode: 'admin',
    sort: 10,
    status: 1,
    remark: '后台运营管理员',
    builtIn: true,
    version: 0,
    createTime: '2026-01-08T09:30:00',
  },
  {
    id: 2,
    roleName: '超级管理员',
    roleCode: 'super_admin',
    sort: 0,
    status: 1,
    remark: '拥有全部权限',
    builtIn: true,
    version: 0,
    createTime: '2026-01-08T09:30:00',
  },
  {
    id: 3,
    roleName: '运营',
    roleCode: 'ops',
    sort: 20,
    status: 1,
    remark: '日常运营与用户维护',
    builtIn: false,
    version: 0,
    createTime: '2026-02-12T14:20:00',
  },
  {
    id: 4,
    roleName: '审计员',
    roleCode: 'audit',
    sort: 30,
    status: 1,
    remark: '只读审计日志',
    builtIn: false,
    version: 0,
    createTime: '2026-03-02T10:10:00',
  },
]

const PERMISSION_DEFS = [
  ['用户列表', 'system:user:list'],
  ['新增用户', 'system:user:add'],
  ['编辑用户', 'system:user:edit'],
  ['删除用户', 'system:user:delete'],
  ['启停用户', 'system:user:status'],
  ['重置密码', 'system:user:reset-password'],
  ['分配角色', 'system:user:assign-role'],
  ['角色列表', 'system:role:list'],
  ['新增角色', 'system:role:add'],
  ['编辑角色', 'system:role:edit'],
  ['删除角色', 'system:role:delete'],
  ['启停角色', 'system:role:status'],
  ['分配菜单', 'system:role:assign-menu'],
  ['分配权限', 'system:role:assign-permission'],
  ['菜单列表', 'system:menu:list'],
  ['新增菜单', 'system:menu:add'],
  ['编辑菜单', 'system:menu:edit'],
  ['删除菜单', 'system:menu:delete'],
  ['启停菜单', 'system:menu:status'],
  ['权限点列表', 'system:permission:list'],
  ['新增权限点', 'system:permission:add'],
  ['编辑权限点', 'system:permission:edit'],
  ['删除权限点', 'system:permission:delete'],
  ['启停权限点', 'system:permission:status'],
  ['字典列表', 'system:dict:list'],
  ['新增字典', 'system:dict:add'],
  ['编辑字典', 'system:dict:edit'],
  ['删除字典', 'system:dict:delete'],
  ['参数列表', 'system:config:list'],
  ['新增参数', 'system:config:add'],
  ['编辑参数', 'system:config:edit'],
  ['删除参数', 'system:config:delete'],
  ['公告列表', 'system:notice:list'],
  ['新增公告', 'system:notice:add'],
  ['编辑公告', 'system:notice:edit'],
  ['删除公告', 'system:notice:delete'],
  ['上传文件', 'system:file:upload'],
  ['文件列表', 'system:file:list'],
  ['删除文件', 'system:file:delete'],
  ['操作日志', 'system:operlog:list'],
  ['登录日志', 'system:loginlog:list'],
]

const MENU_DEFS = [
  [100, 0, '系统管理', 'M', '/system', null, null, 'Settings', 1, 1, 1],
  [101, 100, '用户管理', 'C', '/system/user', 'system/user/index', 'system:user:list', 'User', 1, 1, 1],
  [102, 100, '角色管理', 'C', '/system/role', 'system/role/index', 'system:role:list', 'Team', 2, 1, 1],
  [103, 100, '菜单管理', 'C', '/system/menu', 'system/menu/index', 'system:menu:list', 'Menu', 3, 1, 1],
  [104, 100, '权限点', 'C', '/system/permission', 'system/permission/index', 'system:permission:list', 'Key', 4, 1, 1],
  [105, 100, '字典管理', 'C', '/system/dict', 'system/dict/index', 'system:dict:list', 'Book', 5, 1, 1],
  [106, 100, '参数配置', 'C', '/system/config', 'system/config/index', 'system:config:list', 'SetUp', 6, 1, 1],
  [107, 100, '公告管理', 'C', '/system/notice', 'system/notice/index', 'system:notice:list', 'Bell', 7, 1, 1],
  [108, 100, '文件管理', 'C', '/system/file', 'system/file/index', 'system:file:list', 'Folder', 8, 1, 1],
  [109, 100, '操作日志', 'C', '/system/oper-log', 'system/operlog/index', 'system:operlog:list', 'Document', 9, 1, 1],
  [110, 100, '登录日志', 'C', '/system/login-log', 'system/loginlog/index', 'system:loginlog:list', 'Clock', 10, 1, 1],
]

const USER_SEEDS = [
  ['admin', '管理员', '13812348000', 'admin@routego.dev', [2], 1],
  ['ops', '运营小王', '13812348001', 'ops@routego.dev', [3], 1],
  ['zhaoli', '赵丽', '13812348002', 'zhaoli@routego.dev', [3], 1],
  ['sunqiang', '孙强', '13812348003', 'sunqiang@routego.dev', [3], 1],
  ['zhoumin', '周敏', '13812348004', 'zhoumin@routego.dev', [4], 1],
  ['wufan', '吴凡', '13812348005', 'wufan@routego.dev', [3], 1],
  ['zhengshuang', '郑爽', '13812348006', 'zhengshuang@routego.dev', [4], 0],
  ['wanglu', '王璐', '13812348007', 'wanglu@routego.dev', [3], 1],
  ['fengyu', '冯宇', '13812348008', 'fengyu@routego.dev', [3], 1],
  ['chenxi', '陈曦', '13812348009', 'chenxi@routego.dev', [4], 1],
  ['chuyang', '褚阳', '13812348010', 'chuyang@routego.dev', [3], 0],
  ['weiqi', '卫琪', '13812348011', 'weiqi@routego.dev', [3], 1],
  ['jiangnan', '蒋楠', '13812348012', 'jiangnan@routego.dev', [4], 1],
  ['shenyu', '沈宇', '13812348013', 'shenyu@routego.dev', [3], 1],
  ['hanxue', '韩雪', '13812348014', 'hanxue@routego.dev', [3], 1],
  ['yangfan', '杨帆', '13812348015', 'yangfan@routego.dev', [4], 1],
  ['zhujing', '朱静', '13812348016', 'zhujing@routego.dev', [3], 1],
  ['qinyue', '秦悦', '13812348017', 'qinyue@routego.dev', [3], 0],
  ['youshan', '尤山', '13812348018', 'youshan@routego.dev', [4], 1],
  ['xulei', '徐磊', '13812348019', 'xulei@routego.dev', [3], 1],
  ['hemin', '何敏', '13812348020', 'hemin@routego.dev', [3], 1],
  ['gaoyuan', '高远', '13812348021', 'gaoyuan@routego.dev', [4], 1],
  ['linxi', '林夕', '13812348022', 'linxi@routego.dev', [3], 1],
  ['luoyi', '罗一', '13812348023', 'luoyi@routego.dev', [4], 1],
]

const LOGIN_IPS = ['10.12.3.18', '10.12.3.24', '172.16.8.9', '192.168.1.44']

function iso(daysAgo, hour, minute) {
  const date = new Date(2026, 10, 4, hour, minute, 0)
  date.setDate(date.getDate() - daysAgo)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}:00`
}

function buildUsers() {
  const base = USER_SEEDS.map(([username, nickname, phone, email, roleIds, status], index) => ({
    id: index + 1,
    username,
    password: username === 'admin' ? 'Admin123!' : username === 'ops' ? 'Ops12345' : 'Passw0rd!',
    nickname,
    avatar: '',
    phone,
    email,
    status,
    lastLoginTime: iso(index % 9, 7 + (index % 12), (index * 7) % 60),
    lastLoginIp: LOGIN_IPS[index % LOGIN_IPS.length],
    remark: '',
    version: 0,
    createTime: iso(40 - index, 9, (index * 11) % 60),
    roleIds,
  }))
  return base
}

function buildMenus() {
  return MENU_DEFS.map(
    ([id, parentId, menuName, menuType, path, component, perms, icon, sort, visible, status]) => ({
      id,
      parentId,
      menuName,
      menuType,
      path,
      component,
      perms,
      icon,
      sort,
      visible,
      status,
      version: 0,
      createTime: '2026-01-08T09:20:00',
    })
  )
}

function buildPermissions() {
  return PERMISSION_DEFS.map(([permissionName, permissionCode], index) => ({
    id: index + 1,
    permissionName,
    permissionCode,
    module: 'system',
    description: '',
    status: 1,
    version: 0,
    createTime: '2026-01-08T09:10:00',
  }))
}

function buildRoleMenus() {
  return {
    1: MENU_DEFS.map(([id]) => id),
    2: MENU_DEFS.map(([id]) => id),
    3: [100, 101],
    4: [100, 109, 110],
  }
}

function buildRolePermissions() {
  const all = PERMISSION_DEFS.map((_, index) => index + 1)
  return {
    1: all,
    2: all,
    3: [1, 8],
    4: [1, 40, 41],
  }
}

function buildDashboard() {
  return {
    kpis: [
      { key: 'logins', label: '今日登录', value: 128, delta: 12.4, unit: '次', hint: '较昨日' },
      { key: 'online', label: '在线用户', value: 23, delta: 3, unit: '人', hint: '近 5 分钟活跃' },
      { key: 'admins', label: '管理员总数', value: 24, delta: 1, unit: '人', hint: '含 2 个内置账号' },
      { key: 'abnormal', label: '7 日异常登录', value: 6, delta: -2, unit: '次', hint: '失败或锁定' },
    ],
    recentOperations: [
      { id: 901, operTime: iso(0, 10, 42), operName: 'admin', title: '编辑用户', method: 'PUT /admin/system/user/7', status: 1, costTime: 42 },
      { id: 900, operTime: iso(0, 10, 31), operName: 'ops', title: '分配角色', method: 'PUT /admin/system/user/12/roles', status: 1, costTime: 31 },
      { id: 899, operTime: iso(0, 10, 18), operName: 'admin', title: '重置密码', method: 'PUT /admin/system/user/5/password', status: 1, costTime: 55 },
      { id: 898, operTime: iso(0, 9, 56), operName: 'audit', title: '导出日志', method: 'GET /admin/system/oper-log/page', status: 0, costTime: 128 },
      { id: 897, operTime: iso(0, 9, 41), operName: 'admin', title: '新增角色', method: 'POST /admin/system/role', status: 1, costTime: 33 },
      { id: 896, operTime: iso(0, 9, 22), operName: 'ops', title: '停用用户', method: 'PUT /admin/system/user/11/status', status: 1, costTime: 27 },
      { id: 895, operTime: iso(1, 18, 12), operName: 'admin', title: '分配权限', method: 'PUT /admin/system/role/3/permissions', status: 1, costTime: 61 },
      { id: 894, operTime: iso(1, 17, 48), operName: 'admin', title: '新增菜单', method: 'POST /admin/system/menu', status: 0, costTime: 403 },
    ],
    loginFailures: [
      { id: 501, username: 'root', ip: '45.88.12.6', reason: '账号或密码错误', time: iso(0, 10, 5), count: 5 },
      { id: 500, username: 'chuyang', ip: '10.12.3.24', reason: '账号已禁用', time: iso(0, 8, 40), count: 2 },
      { id: 499, username: 'ops', ip: '172.16.8.9', reason: '验证码错误', time: iso(1, 16, 22), count: 1 },
      { id: 498, username: 'admin', ip: '45.88.12.6', reason: '账号或密码错误', time: iso(1, 3, 11), count: 3 },
    ],
    systemStatus: [
      { key: 'service', label: 'API 服务', value: '正常', status: 'success', detail: '延迟 38ms' },
      { key: 'cache', label: 'Redis 缓存', value: '正常', status: 'success', detail: '命中率 98.2%' },
      { key: 'storage', label: '对象存储', value: '正常', status: 'success', detail: '已用 12.4 GB' },
      { key: 'sync', label: '最后同步', value: '10:42:18', status: 'info', detail: '菜单与权限缓存' },
    ],
  }
}

function createInitial() {
  return {
    users: buildUsers(),
    roles: ROLE_DEFS.map((role) => ({ ...role })),
    menus: buildMenus(),
    permissions: buildPermissions(),
    roleMenus: buildRoleMenus(),
    rolePermissions: buildRolePermissions(),
    notices: [
      { id: 1, title: '11 月系统维护窗口', content: '11 月 9 日 02:00–04:00 进行缓存迁移，期间后台只读。', type: 1, status: 1, isTop: 1, publishTime: iso(0, 8, 30) },
      { id: 2, title: '新增权限点审计规范', content: '新增权限点必须绑定模块并说明用途。', type: 2, status: 1, isTop: 0, publishTime: iso(2, 15, 10) },
      { id: 3, title: '管理员密码策略升级', content: '密码有效期调整为 90 天，请及时修改。', type: 1, status: 1, isTop: 0, publishTime: iso(4, 9, 0) },
    ],
    dashboard: buildDashboard(),
    sessions: new Map(),
    captchas: new Map(),
    currentUserId: 1,
    sequence: { user: 100, role: 100, menu: 200, permission: 200 },
  }
}

export const state = {}

export function resetDb() {
  Object.assign(state, createInitial())
}

resetDb()

export function paginate(list, pageNum = 1, pageSize = 10) {
  const size = Math.min(Math.max(Number(pageSize) || 10, 1), 200)
  const page = Math.max(Number(pageNum) || 1, 1)
  const total = list.length
  const pages = Math.ceil(total / size)
  const start = (page - 1) * size
  return {
    records: list.slice(start, start + size),
    total,
    pageNum: page,
    pageSize: size,
    pages,
  }
}

export function roleById(id) {
  return state.roles.find((role) => role.id === Number(id))
}

export function userById(id) {
  return state.users.find((user) => user.id === Number(id))
}

export function userRoleIds(userId) {
  const user = userById(userId)
  return user ? [...user.roleIds] : []
}

export function userRoleNames(userId) {
  const ids = userRoleIds(userId)
  return state.roles.filter((role) => ids.includes(role.id)).map((role) => role.roleName)
}

export function permissionCodesForUser(userId) {
  const user = userById(userId)
  if (!user) return []
  const roles = state.roles.filter((role) => user.roleIds.includes(role.id))
  if (roles.some((role) => role.roleCode === 'super_admin')) return ['*']
  const permissionIds = new Set()
  roles.forEach((role) => {
    ;(state.rolePermissions[role.id] || []).forEach((id) => permissionIds.add(id))
  })
  return state.permissions
    .filter((permission) => permissionIds.has(permission.id) && permission.status === 1)
    .map((permission) => permission.permissionCode)
}

export function hasPermissionForUser(userId, permissionCode) {
  if (!permissionCode) return true
  const codes = permissionCodesForUser(userId)
  return codes.includes('*') || codes.includes(permissionCode)
}

export function userCountForRole(roleId) {
  return state.users.filter((user) => user.roleIds.includes(Number(roleId))).length
}

export function childCountForMenu(menuId) {
  return state.menus.filter((menu) => menu.parentId === Number(menuId)).length
}

export function roleCountForPermission(permissionId) {
  return Object.values(state.rolePermissions).filter((ids) =>
    ids.includes(Number(permissionId))
  ).length
}

export function buildMenuTree(menus = state.menus) {
  const nodes = menus.map((menu) => ({
    ...menu,
    childCount: childCountForMenu(menu.id),
    children: [],
  }))
  const map = new Map(nodes.map((node) => [node.id, node]))
  const roots = []
  nodes.forEach((node) => {
    if (node.parentId && map.has(node.parentId)) {
      map.get(node.parentId).children.push(node)
    } else if (node.parentId === 0) {
      roots.push(node)
    }
  })
  const sortNodes = (list) => {
    list.sort((a, b) => a.sort - b.sort)
    list.forEach((item) => sortNodes(item.children))
  }
  sortNodes(roots)
  return roots
}
