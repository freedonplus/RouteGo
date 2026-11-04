export const menuComponents = {
  'system/user/index': () => import('@/views/system/user/UserView.vue'),
  'system/role/index': () => import('@/views/system/role/RoleView.vue'),
  'system/menu/index': () => import('@/views/system/menu/MenuView.vue'),
  'system/permission/index': () => import('@/views/system/permission/PermissionView.vue'),
  'system/dict/index': () => import('@/views/system/dict/DictView.vue'),
  'system/config/index': () => import('@/views/system/config/ConfigView.vue'),
  'system/notice/index': () => import('@/views/system/notice/NoticeView.vue'),
  'system/file/index': () => import('@/views/system/file/FileView.vue'),
  'system/operlog/index': () => import('@/views/system/operlog/OperLogView.vue'),
  'system/loginlog/index': () => import('@/views/system/loginlog/LoginLogView.vue'),
}

export function resolveMenuComponent(component) {
  return component ? menuComponents[component] || null : null
}
