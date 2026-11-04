export const menuComponents = {
  'system/user/index': () => import('@/views/system/user/UserView.vue'),
  'system/role/index': () => import('@/views/system/role/RoleView.vue'),
  'system/menu/index': () => import('@/views/system/menu/MenuView.vue'),
  'system/permission/index': () => import('@/views/system/permission/PermissionView.vue'),
}

export function resolveMenuComponent(component) {
  return component ? menuComponents[component] || null : null
}
