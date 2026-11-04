import {
  state,
  paginate,
  userById,
  roleById,
  userRoleIds,
  userRoleNames,
  permissionCodesForUser,
  hasPermissionForUser,
  userCountForRole,
  roleCountForPermission,
  childCountForMenu,
  buildMenuTree,
} from './db'
import { createCaptcha, verifyCaptcha } from './captcha'
import { maskPhone, maskEmail, isMasked } from '@/utils/format'

class MockError extends Error {
  constructor(code, message) {
    super(message)
    this.code = code
  }
}

if (import.meta.env.DEV && typeof window !== 'undefined') {
  // Dev-only handle for manual/automated QA of the in-memory mock.
  window.__RG_MOCK__ = { state }
}

const delay = () =>
  new Promise((resolve) => setTimeout(resolve, 150 + Math.random() * 200))

function ok(data, config) {
  return {
    data: { code: 200, msg: 'success', data, timestamp: Date.now() },
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
  }
}

function fail(code, msg, config) {
  return {
    data: { code, msg, data: null, timestamp: Date.now() },
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
  }
}

function headerValue(config, name) {
  const headers = config.headers || {}
  const value = headers[name] || headers[name.toLowerCase()]
  if (value) return String(value)
  if (typeof headers.get === 'function') return headers.get(name) || ''
  return ''
}

function sessionUser(config) {
  const raw = headerValue(config, 'Authorization')
  const token = raw.replace(/^Bearer\s+/i, '')
  const userId = state.sessions.get(token)
  if (userId) return userById(userId)
  // Mock-only convenience: a session token survives a full page reload even
  // though the in-memory session map does not.
  const match = token.match(/^mock-token-(\d+)-/)
  return match ? userById(match[1]) || null : null
}

function parseBody(data) {
  if (!data) return {}
  if (typeof data === 'string') {
    try {
      return JSON.parse(data)
    } catch {
      return {}
    }
  }
  return data
}

function query(config) {
  const params = { ...(config.params || {}) }
  const rawUrl = config.url || ''
  const search = rawUrl.split('?')[1]
  if (search) {
    new URLSearchParams(search).forEach((value, key) => {
      params[key] = value
    })
  }
  return params
}

function pathOf(config) {
  return (config.url || '').split('?')[0].replace(/\/+$/, '') || '/'
}

function requireUser(config) {
  const user = sessionUser(config)
  if (!user) throw new MockError(401, '未登录或登录已过期')
  return user
}

function requirePerm(user, code) {
  if (!hasPermissionForUser(user.id, code)) throw new MockError(403, '没有操作权限')
}

function toInt(value, fallback = 0) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function userVO(user) {
  return {
    id: user.id,
    username: user.username,
    nickname: user.nickname,
    avatar: user.avatar,
    phone: maskPhone(user.phone),
    email: maskEmail(user.email),
    status: user.status,
    lastLoginTime: user.lastLoginTime,
    lastLoginIp: user.lastLoginIp,
    remark: user.remark,
    version: user.version,
    createTime: user.createTime,
    roleNames: userRoleNames(user.id),
  }
}

function userDetailVO(user) {
  return { ...userVO(user), roleIds: userRoleIds(user.id) }
}

function roleVO(role) {
  return {
    id: role.id,
    roleName: role.roleName,
    roleCode: role.roleCode,
    sort: role.sort,
    status: role.status,
    remark: role.remark,
    builtIn: role.builtIn,
    userCount: userCountForRole(role.id),
    version: role.version,
    createTime: role.createTime,
  }
}

function permissionVO(permission) {
  return { ...permission, roleCount: roleCountForPermission(permission.id) }
}

function nextId(kind) {
  state.sequence[kind] += 1
  return state.sequence[kind]
}

function nowIso() {
  const date = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function assertVersion(record, version) {
  if (!record) throw new MockError(404, '记录不存在')
  if (version !== undefined && version !== null && Number(version) !== record.version) {
    throw new MockError(409, '数据已被修改，请刷新后重试')
  }
}

function assertPassword(password) {
  if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d!@#$%^&*._-]{8,20}$/.test(String(password || ''))) {
    throw new MockError(400, '密码需 8–20 位且至少包含字母和数字')
  }
}

function visibleMenusFor(user) {
  if (permissionCodesForUser(user.id).includes('*')) {
    return state.menus.filter((menu) => menu.status === 1 && menu.visible === 1)
  }
  const allowed = new Set()
  state.menus.forEach((menu) => {
    if (menu.perms && hasPermissionForUser(user.id, menu.perms)) allowed.add(menu.id)
  })
  const tree = buildMenuTree(state.menus.filter((m) => m.status === 1 && m.visible === 1))
  const keep = (node) => {
    const children = (node.children || []).filter(keep)
    const keepSelf = allowed.has(node.id) || children.length > 0
    node.children = children
    return keepSelf
  }
  return tree.filter(keep)
}

const handlers = []

function route(method, pattern, handler) {
  handlers.push({ method, pattern, handler })
}

/* ---------- auth ---------- */

route('get', /^\/admin\/auth\/captcha$/, (ctx) => {
  const captcha = createCaptcha()
  return ok({ uuid: captcha.uuid, image: captcha.image }, ctx.config)
})

route('post', /^\/admin\/auth\/login$/, ({ body, config }) => {
  if (!verifyCaptcha(body.captchaUuid, body.captchaCode)) {
    throw new MockError(400, '验证码错误或已过期')
  }
  const username = String(body.username || '').trim()
  const user = state.users.find((item) => item.username === username)
  if (!user || user.password !== body.password) {
    throw new MockError(401, '账号或密码错误')
  }
  if (user.status !== 1) throw new MockError(403, '账号已禁用')
  const token = `mock-token-${user.id}-${Math.random().toString(16).slice(2)}`
  state.sessions.set(token, user.id)
  state.currentUserId = user.id
  return ok(
    {
      tokenName: 'Authorization',
      tokenValue: token,
      userId: user.id,
      username: user.username,
      nickname: user.nickname,
    },
    config
  )
})

route('post', /^\/admin\/auth\/logout$/, ({ config }) => {
  const raw = headerValue(config, 'Authorization').replace(/^Bearer\s+/i, '')
  state.sessions.delete(raw)
  return ok(null, config)
})

route('get', /^\/admin\/auth\/me$/, ({ config }) => {
  const user = requireUser(config)
  return ok(
    {
      userId: user.id,
      username: user.username,
      nickname: user.nickname,
      avatar: user.avatar,
      roles: state.roles.filter((role) => user.roleIds.includes(role.id)).map((r) => r.roleCode),
      permissions: permissionCodesForUser(user.id),
    },
    config
  )
})

route('put', /^\/admin\/auth\/password$/, ({ body, config }) => {
  const user = requireUser(config)
  if (user.password !== body.oldPassword) throw new MockError(400, '旧密码不正确')
  assertPassword(body.newPassword)
  user.password = body.newPassword
  return ok(null, config)
})

/* ---------- dashboard ---------- */

route('get', /^\/admin\/dashboard\/overview$/, ({ config }) => {
  requireUser(config)
  return ok(state.dashboard, config)
})

/* ---------- users ---------- */

route('get', /^\/admin\/system\/user\/page$/, ({ config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:user:list')
  const q = query(config)
  let list = [...state.users]
  if (q.username) list = list.filter((item) => item.username.includes(q.username))
  if (q.nickname) list = list.filter((item) => item.nickname.includes(q.nickname))
  if (q.phone) list = list.filter((item) => item.phone.includes(q.phone))
  if (q.status !== undefined && q.status !== '') {
    list = list.filter((item) => Number(item.status) === Number(q.status))
  }
  if (q.roleId) list = list.filter((item) => item.roleIds.includes(Number(q.roleId)))
  if (q.createTimeStart) list = list.filter((item) => item.createTime >= q.createTimeStart)
  if (q.createTimeEnd) list = list.filter((item) => item.createTime <= q.createTimeEnd)
  list.sort((a, b) => b.id - a.id)
  const page = paginate(list, q.pageNum, q.pageSize)
  return ok({ ...page, records: page.records.map(userVO) }, config)
})

route('get', /^\/admin\/system\/user\/(\d+)$/, ({ match, config }) => {
  const current = requireUser(config)
  requirePerm(current, 'system:user:list')
  const user = userById(match[1])
  if (!user) throw new MockError(404, '用户不存在')
  return ok(userDetailVO(user), config)
})

route('post', /^\/admin\/system\/user$/, ({ body, config }) => {
  const current = requireUser(config)
  requirePerm(current, 'system:user:add')
  const username = String(body.username || '').trim()
  if (!/^[A-Za-z0-9_]{3,50}$/.test(username)) {
    throw new MockError(400, '账号需为 3–50 位字母、数字或下划线')
  }
  if (state.users.some((item) => item.username === username)) {
    throw new MockError(409, '账号已存在')
  }
  assertPassword(body.password)
  if (!Array.isArray(body.roleIds) || body.roleIds.length === 0) {
    throw new MockError(400, '至少选择一个角色')
  }
  const id = nextId('user')
  state.users.push({
    id,
    username,
    password: body.password,
    nickname: body.nickname || username,
    avatar: body.avatar || '',
    phone: body.phone || '',
    email: body.email || '',
    status: 1,
    lastLoginTime: null,
    lastLoginIp: '',
    remark: body.remark || '',
    version: 0,
    createTime: nowIso(),
    roleIds: body.roleIds.map(Number),
  })
  return ok(id, config)
})

route('put', /^\/admin\/system\/user\/(\d+)$/, ({ match, body, config }) => {
  const current = requireUser(config)
  requirePerm(current, 'system:user:edit')
  const user = userById(match[1])
  assertVersion(user, body.version)
  if (body.nickname !== undefined) user.nickname = body.nickname
  if (body.avatar !== undefined) user.avatar = body.avatar
  if (body.phone !== undefined && body.phone && !isMasked(body.phone)) user.phone = body.phone
  if (body.email !== undefined && body.email && !isMasked(body.email)) user.email = body.email
  if (body.remark !== undefined) user.remark = body.remark
  user.version += 1
  return ok(null, config)
})

route('put', /^\/admin\/system\/user\/(\d+)\/status$/, ({ match, config }) => {
  const current = requireUser(config)
  requirePerm(current, 'system:user:status')
  const user = userById(match[1])
  if (!user) throw new MockError(404, '用户不存在')
  if (user.id === current.id) throw new MockError(400, '不能停用自己')
  if (user.username === 'admin') throw new MockError(400, '受保护账号不能停用')
  user.status = toInt(query(config).status, 1) ? 1 : 0
  return ok(null, config)
})

route('put', /^\/admin\/system\/user\/(\d+)\/password$/, ({ match, body, config }) => {
  const current = requireUser(config)
  requirePerm(current, 'system:user:reset-password')
  const user = userById(match[1])
  if (!user) throw new MockError(404, '用户不存在')
  assertPassword(body.newPassword)
  user.password = body.newPassword
  return ok(null, config)
})

route('get', /^\/admin\/system\/user\/(\d+)\/roles$/, ({ match, config }) => {
  const current = requireUser(config)
  requirePerm(current, 'system:user:list')
  return ok(userRoleIds(match[1]), config)
})

route('put', /^\/admin\/system\/user\/(\d+)\/roles$/, ({ match, body, config }) => {
  const current = requireUser(config)
  requirePerm(current, 'system:user:assign-role')
  const user = userById(match[1])
  if (!user) throw new MockError(404, '用户不存在')
  if (!Array.isArray(body.roleIds) || body.roleIds.length === 0) {
    throw new MockError(400, '至少选择一个角色')
  }
  user.roleIds = body.roleIds.map(Number)
  return ok(null, config)
})

route('delete', /^\/admin\/system\/user\/(\d+)$/, ({ match, config }) => {
  const current = requireUser(config)
  requirePerm(current, 'system:user:delete')
  const user = userById(match[1])
  if (!user) throw new MockError(404, '用户不存在')
  if (user.id === current.id) throw new MockError(400, '不能删除自己')
  if (user.username === 'admin') throw new MockError(400, '超级管理员不能删除')
  state.users = state.users.filter((item) => item.id !== user.id)
  return ok(null, config)
})

/* ---------- roles ---------- */

route('get', /^\/admin\/system\/role\/page$/, ({ config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:role:list')
  const q = query(config)
  let list = [...state.roles]
  if (q.roleName) list = list.filter((item) => item.roleName.includes(q.roleName))
  if (q.roleCode) list = list.filter((item) => item.roleCode.includes(q.roleCode))
  if (q.status !== undefined && q.status !== '') {
    list = list.filter((item) => Number(item.status) === Number(q.status))
  }
  list.sort((a, b) => a.sort - b.sort)
  const page = paginate(list, q.pageNum, q.pageSize)
  return ok({ ...page, records: page.records.map(roleVO) }, config)
})

route('get', /^\/admin\/system\/role\/(\d+)\/menus$/, ({ match, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:role:list')
  return ok(state.roleMenus[match[1]] || [], config)
})

route('put', /^\/admin\/system\/role\/(\d+)\/menus$/, ({ match, body, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:role:assign-menu')
  if (!roleById(match[1])) throw new MockError(404, '角色不存在')
  state.roleMenus[match[1]] = (body.ids || []).map(Number)
  return ok(null, config)
})

route('get', /^\/admin\/system\/role\/(\d+)\/permissions$/, ({ match, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:role:list')
  return ok(state.rolePermissions[match[1]] || [], config)
})

route('put', /^\/admin\/system\/role\/(\d+)\/permissions$/, ({ match, body, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:role:assign-permission')
  if (!roleById(match[1])) throw new MockError(404, '角色不存在')
  state.rolePermissions[match[1]] = (body.ids || []).map(Number)
  return ok(null, config)
})

route('get', /^\/admin\/system\/role\/(\d+)\/users$/, ({ match, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:role:list')
  const q = query(config)
  const list = state.users.filter((item) => item.roleIds.includes(Number(match[1])))
  const page = paginate(list, q.pageNum, q.pageSize)
  return ok({ ...page, records: page.records.map(userVO) }, config)
})

route('get', /^\/admin\/system\/role\/(\d+)$/, ({ match, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:role:list')
  const role = roleById(match[1])
  if (!role) throw new MockError(404, '角色不存在')
  return ok(
    {
      ...roleVO(role),
      menuIds: state.roleMenus[role.id] || [],
      permissionIds: state.rolePermissions[role.id] || [],
    },
    config
  )
})

route('post', /^\/admin\/system\/role$/, ({ body, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:role:add')
  const roleCode = String(body.roleCode || '').trim()
  if (!roleCode) throw new MockError(400, '角色编码不能为空')
  if (state.roles.some((item) => item.roleCode === roleCode)) {
    throw new MockError(409, '角色编码已存在')
  }
  const id = nextId('role')
  state.roles.push({
    id,
    roleName: body.roleName,
    roleCode,
    sort: toInt(body.sort, 0),
    status: body.status === undefined ? 1 : Number(body.status),
    remark: body.remark || '',
    builtIn: false,
    version: 0,
    createTime: nowIso(),
  })
  state.roleMenus[id] = []
  state.rolePermissions[id] = []
  return ok(id, config)
})

route('put', /^\/admin\/system\/role\/(\d+)\/status$/, ({ match, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:role:status')
  const role = roleById(match[1])
  if (!role) throw new MockError(404, '角色不存在')
  if (role.builtIn) throw new MockError(400, '内置角色不能停用')
  role.status = toInt(query(config).status, 1) ? 1 : 0
  return ok(null, config)
})

route('put', /^\/admin\/system\/role\/(\d+)$/, ({ match, body, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:role:edit')
  const role = roleById(match[1])
  assertVersion(role, body.version)
  if (body.roleName !== undefined) role.roleName = body.roleName
  if (body.sort !== undefined) role.sort = toInt(body.sort, role.sort)
  if (body.status !== undefined) role.status = Number(body.status)
  if (body.remark !== undefined) role.remark = body.remark
  role.version += 1
  return ok(null, config)
})

route('delete', /^\/admin\/system\/role\/(\d+)$/, ({ match, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:role:delete')
  const role = roleById(match[1])
  if (!role) throw new MockError(404, '角色不存在')
  if (role.builtIn) throw new MockError(400, '内置角色不能删除')
  if (userCountForRole(role.id) > 0) throw new MockError(409, '角色下仍有用户，不能删除')
  state.roles = state.roles.filter((item) => item.id !== role.id)
  delete state.roleMenus[role.id]
  delete state.rolePermissions[role.id]
  return ok(null, config)
})

/* ---------- menus ---------- */

route('get', /^\/admin\/system\/menu\/tree$/, ({ config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:menu:list')
  return ok(buildMenuTree(), config)
})

route('get', /^\/admin\/system\/menu\/user-tree$/, ({ config }) => {
  const user = requireUser(config)
  return ok(visibleMenusFor(user), config)
})

route('get', /^\/admin\/system\/menu\/(\d+)$/, ({ match, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:menu:list')
  const menu = state.menus.find((item) => item.id === Number(match[1]))
  if (!menu) throw new MockError(404, '菜单不存在')
  return ok(menu, config)
})

route('post', /^\/admin\/system\/menu$/, ({ body, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:menu:add')
  const id = nextId('menu')
  state.menus.push({
    id,
    parentId: toInt(body.parentId, 0),
    menuName: body.menuName,
    menuType: body.menuType || 'C',
    path: body.path || '',
    component: body.component || null,
    perms: body.perms || null,
    icon: body.icon || '',
    sort: toInt(body.sort, 0),
    visible: body.visible === undefined ? 1 : Number(body.visible),
    status: body.status === undefined ? 1 : Number(body.status),
    version: 0,
    createTime: nowIso(),
  })
  return ok(id, config)
})

route('put', /^\/admin\/system\/menu\/(\d+)\/status$/, ({ match, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:menu:status')
  const menu = state.menus.find((item) => item.id === Number(match[1]))
  if (!menu) throw new MockError(404, '菜单不存在')
  menu.status = toInt(query(config).status, 1) ? 1 : 0
  return ok(null, config)
})

route('put', /^\/admin\/system\/menu\/(\d+)\/visible$/, ({ match, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:menu:edit')
  const menu = state.menus.find((item) => item.id === Number(match[1]))
  if (!menu) throw new MockError(404, '菜单不存在')
  menu.visible = toInt(query(config).visible, 1) ? 1 : 0
  return ok(null, config)
})

route('put', /^\/admin\/system\/menu\/(\d+)$/, ({ match, body, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:menu:edit')
  const menu = state.menus.find((item) => item.id === Number(match[1]))
  assertVersion(menu, body.version)
  const parentId = toInt(body.parentId, menu.parentId)
  if (parentId === menu.id) throw new MockError(400, '上级菜单不能是自己')
  let cursor = state.menus.find((item) => item.id === parentId)
  while (cursor) {
    if (cursor.id === menu.id) throw new MockError(400, '上级菜单不能是自己的子级')
    cursor = state.menus.find((item) => item.id === cursor.parentId)
  }
  Object.assign(menu, {
    parentId,
    menuName: body.menuName ?? menu.menuName,
    menuType: body.menuType ?? menu.menuType,
    path: body.path ?? menu.path,
    component: body.component ?? menu.component,
    perms: body.perms ?? menu.perms,
    icon: body.icon ?? menu.icon,
    sort: body.sort !== undefined ? toInt(body.sort, menu.sort) : menu.sort,
    visible: body.visible !== undefined ? Number(body.visible) : menu.visible,
    status: body.status !== undefined ? Number(body.status) : menu.status,
  })
  menu.version += 1
  return ok(null, config)
})

route('delete', /^\/admin\/system\/menu\/(\d+)$/, ({ match, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:menu:delete')
  const id = Number(match[1])
  if (!state.menus.some((item) => item.id === id)) throw new MockError(404, '菜单不存在')
  if (childCountForMenu(id) > 0) throw new MockError(409, '存在子菜单，不能删除')
  const referenced = Object.values(state.roleMenus).some((ids) => ids.includes(id))
  if (referenced) throw new MockError(409, '角色仍引用该菜单，不能删除')
  state.menus = state.menus.filter((item) => item.id !== id)
  return ok(null, config)
})

/* ---------- permissions ---------- */

route('get', /^\/admin\/system\/permission\/page$/, ({ config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:permission:list')
  const q = query(config)
  let list = [...state.permissions]
  if (q.permissionName) list = list.filter((item) => item.permissionName.includes(q.permissionName))
  if (q.permissionCode) list = list.filter((item) => item.permissionCode.includes(q.permissionCode))
  if (q.module) list = list.filter((item) => item.module === q.module)
  if (q.status !== undefined && q.status !== '') {
    list = list.filter((item) => Number(item.status) === Number(q.status))
  }
  list.sort((a, b) => a.id - b.id)
  const page = paginate(list, q.pageNum, q.pageSize)
  return ok({ ...page, records: page.records.map(permissionVO) }, config)
})

route('get', /^\/admin\/system\/permission\/grouped$/, ({ config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:permission:list')
  const modules = new Map()
  state.permissions.forEach((permission) => {
    if (!modules.has(permission.module)) modules.set(permission.module, [])
    modules.get(permission.module).push(permissionVO(permission))
  })
  return ok(
    [...modules.entries()].map(([module, permissions]) => ({ module, permissions })),
    config
  )
})

route('get', /^\/admin\/system\/permission\/(\d+)$/, ({ match, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:permission:list')
  const permission = state.permissions.find((item) => item.id === Number(match[1]))
  if (!permission) throw new MockError(404, '权限点不存在')
  return ok(permissionVO(permission), config)
})

route('post', /^\/admin\/system\/permission$/, ({ body, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:permission:add')
  const code = String(body.permissionCode || '').trim()
  if (!code) throw new MockError(400, '权限码不能为空')
  if (state.permissions.some((item) => item.permissionCode === code)) {
    throw new MockError(409, '权限码已存在')
  }
  const id = nextId('permission')
  state.permissions.push({
    id,
    permissionName: body.permissionName,
    permissionCode: code,
    module: body.module || 'system',
    description: body.description || '',
    status: body.status === undefined ? 1 : Number(body.status),
    version: 0,
    createTime: nowIso(),
  })
  return ok(id, config)
})

route('put', /^\/admin\/system\/permission\/(\d+)\/status$/, ({ match, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:permission:status')
  const permission = state.permissions.find((item) => item.id === Number(match[1]))
  if (!permission) throw new MockError(404, '权限点不存在')
  permission.status = toInt(query(config).status, 1) ? 1 : 0
  return ok(null, config)
})

route('put', /^\/admin\/system\/permission\/(\d+)$/, ({ match, body, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:permission:edit')
  const permission = state.permissions.find((item) => item.id === Number(match[1]))
  assertVersion(permission, body.version)
  if (body.permissionName !== undefined) permission.permissionName = body.permissionName
  if (body.module !== undefined) permission.module = body.module
  if (body.description !== undefined) permission.description = body.description
  if (body.status !== undefined) permission.status = Number(body.status)
  permission.version += 1
  return ok(null, config)
})

route('delete', /^\/admin\/system\/permission\/(\d+)$/, ({ match, config }) => {
  const user = requireUser(config)
  requirePerm(user, 'system:permission:delete')
  const id = Number(match[1])
  if (!state.permissions.some((item) => item.id === id)) {
    throw new MockError(404, '权限点不存在')
  }
  if (roleCountForPermission(id) > 0) throw new MockError(409, '角色仍引用该权限点，不能删除')
  state.permissions = state.permissions.filter((item) => item.id !== id)
  return ok(null, config)
})

/* ---------- notices ---------- */

route('get', /^\/admin\/system\/notice\/published$/, ({ config }) => {
  requireUser(config)
  const list = state.notices
    .filter((item) => item.status === 1)
    .sort((a, b) => b.isTop - a.isTop || String(b.publishTime).localeCompare(String(a.publishTime)))
  return ok(list, config)
})

/* ---------- adapter ---------- */

export async function mockAdapter(config) {
  await delay()
  const method = (config.method || 'get').toLowerCase()
  const path = pathOf(config)
  const isPublic =
    (method === 'get' && path === '/admin/auth/captcha') ||
    (method === 'post' && path === '/admin/auth/login')

  if (!isPublic && !sessionUser(config)) {
    return fail(401, '未登录或登录已过期', config)
  }

  const entry = handlers.find((item) => item.method === method && item.pattern.test(path))
  if (!entry) return fail(404, `接口不存在：${method.toUpperCase()} ${path}`, config)

  const match = path.match(entry.pattern)
  try {
    return entry.handler({
      match,
      body: parseBody(config.data),
      config,
      params: query(config),
    })
  } catch (error) {
    if (error instanceof MockError) return fail(error.code, error.message, config)
    return fail(500, error?.message || '服务器错误', config)
  }
}

export default mockAdapter
