import { http } from './request'

/* dashboard */
export function getDashboardOverview() {
  return http.get('/admin/dashboard/overview')
}

/* users */
export function pageUsers(params) {
  return http.get('/admin/system/user/page', { params })
}

export function getUser(id) {
  return http.get(`/admin/system/user/${id}`)
}

export function createUser(data) {
  return http.post('/admin/system/user', data)
}

export function updateUser(id, data) {
  return http.put(`/admin/system/user/${id}`, data)
}

export function setUserStatus(id, status) {
  return http.put(`/admin/system/user/${id}/status`, null, { params: { status } })
}

export function resetUserPassword(id, newPassword) {
  return http.put(`/admin/system/user/${id}/password`, { newPassword })
}

export function getUserRoles(id) {
  return http.get(`/admin/system/user/${id}/roles`)
}

export function assignUserRoles(id, roleIds) {
  return http.put(`/admin/system/user/${id}/roles`, { roleIds })
}

export function deleteUser(id) {
  return http.delete(`/admin/system/user/${id}`)
}

/* roles */
export function pageRoles(params) {
  return http.get('/admin/system/role/page', { params })
}

export function getRole(id) {
  return http.get(`/admin/system/role/${id}`)
}

export function createRole(data) {
  return http.post('/admin/system/role', data)
}

export function updateRole(id, data) {
  return http.put(`/admin/system/role/${id}`, data)
}

export function setRoleStatus(id, status) {
  return http.put(`/admin/system/role/${id}/status`, null, { params: { status } })
}

export function deleteRole(id) {
  return http.delete(`/admin/system/role/${id}`)
}

export function getRoleMenus(id) {
  return http.get(`/admin/system/role/${id}/menus`)
}

export function assignRoleMenus(id, ids) {
  return http.put(`/admin/system/role/${id}/menus`, { ids })
}

export function getRolePermissions(id) {
  return http.get(`/admin/system/role/${id}/permissions`)
}

export function assignRolePermissions(id, ids) {
  return http.put(`/admin/system/role/${id}/permissions`, { ids })
}

export function pageRoleUsers(id, params) {
  return http.get(`/admin/system/role/${id}/users`, { params })
}

/* menus */
export function getMenuTree() {
  return http.get('/admin/system/menu/tree')
}

export function getUserMenuTree() {
  return http.get('/admin/system/menu/user-tree')
}

export function getMenu(id) {
  return http.get(`/admin/system/menu/${id}`)
}

export function createMenu(data) {
  return http.post('/admin/system/menu', data)
}

export function updateMenu(id, data) {
  return http.put(`/admin/system/menu/${id}`, data)
}

export function deleteMenu(id) {
  return http.delete(`/admin/system/menu/${id}`)
}

export function setMenuStatus(id, status) {
  return http.put(`/admin/system/menu/${id}/status`, null, { params: { status } })
}

export function setMenuVisible(id, visible) {
  return http.put(`/admin/system/menu/${id}/visible`, null, { params: { visible } })
}

/* permissions */
export function pagePermissions(params) {
  return http.get('/admin/system/permission/page', { params })
}

export function getGroupedPermissions() {
  return http.get('/admin/system/permission/grouped')
}

export function getPermission(id) {
  return http.get(`/admin/system/permission/${id}`)
}

export function createPermission(data) {
  return http.post('/admin/system/permission', data)
}

export function updatePermission(id, data) {
  return http.put(`/admin/system/permission/${id}`, data)
}

export function deletePermission(id) {
  return http.delete(`/admin/system/permission/${id}`)
}

export function setPermissionStatus(id, status) {
  return http.put(`/admin/system/permission/${id}/status`, null, { params: { status } })
}

/* notices */
export function getPublishedNotices() {
  return http.get('/admin/system/notice/published')
}
