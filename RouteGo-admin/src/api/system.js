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

/* dict types */
export function pageDictTypes(params) {
  return http.get('/admin/system/dict/type/page', { params })
}
export function getDictType(id) {
  return http.get(`/admin/system/dict/type/${id}`)
}
export function createDictType(data) {
  return http.post('/admin/system/dict/type', data)
}
export function updateDictType(id, data) {
  return http.put(`/admin/system/dict/type/${id}`, data)
}
export function deleteDictType(id) {
  return http.delete(`/admin/system/dict/type/${id}`)
}
export function setDictTypeStatus(id, status) {
  return http.put(`/admin/system/dict/type/${id}/status`, null, { params: { status } })
}

/* dict data */
export function pageDictData(params) {
  return http.get('/admin/system/dict/data/page', { params })
}
export function listDictDataByType(dictType) {
  return http.get(`/admin/system/dict/data/list/${dictType}`)
}
export function getDictData(id) {
  return http.get(`/admin/system/dict/data/${id}`)
}
export function createDictData(data) {
  return http.post('/admin/system/dict/data', data)
}
export function updateDictData(id, data) {
  return http.put(`/admin/system/dict/data/${id}`, data)
}
export function deleteDictData(id) {
  return http.delete(`/admin/system/dict/data/${id}`)
}
export function setDictDataStatus(id, status) {
  return http.put(`/admin/system/dict/data/${id}/status`, null, { params: { status } })
}
export function setDictDataDefault(id) {
  return http.put(`/admin/system/dict/data/${id}/default`)
}

/* configs */
export function pageConfigs(params) {
  return http.get('/admin/system/config/page', { params })
}
export function getConfig(id) {
  return http.get(`/admin/system/config/${id}`)
}
export function getConfigByKey(configKey) {
  return http.get(`/admin/system/config/key/${configKey}`)
}
export function createConfig(data) {
  return http.post('/admin/system/config', data)
}
export function updateConfig(id, data) {
  return http.put(`/admin/system/config/${id}`, data)
}
export function deleteConfig(id) {
  return http.delete(`/admin/system/config/${id}`)
}
export function batchUpdateConfigs(items) {
  return http.put('/admin/system/config/batch', { items })
}

/* notices */
export function getPublishedNotices() {
  return http.get('/admin/system/notice/published')
}
export function pageNotices(params) {
  return http.get('/admin/system/notice/page', { params })
}
export function getNotice(id) {
  return http.get(`/admin/system/notice/${id}`)
}
export function createNotice(data) {
  return http.post('/admin/system/notice', data)
}
export function updateNotice(id, data) {
  return http.put(`/admin/system/notice/${id}`, data)
}
export function deleteNotice(id) {
  return http.delete(`/admin/system/notice/${id}`)
}
export function publishNotice(id) {
  return http.put(`/admin/system/notice/${id}/publish`)
}
export function unpublishNotice(id) {
  return http.put(`/admin/system/notice/${id}/unpublish`)
}
export function setNoticeTop(id, top) {
  return http.put(`/admin/system/notice/${id}/top`, null, { params: { top } })
}

/* files */
export function pageFiles(params) {
  return http.get('/admin/system/file/page', { params })
}
export function getFile(id) {
  return http.get(`/admin/system/file/${id}`)
}
export function uploadFile(file) {
  const form = new FormData()
  form.append('file', file)
  return http.post('/admin/system/file/upload', form)
}
export function deleteFile(id) {
  return http.delete(`/admin/system/file/${id}`)
}
export function downloadFile(id) {
  return http.get(`/admin/system/file/${id}/download`, { responseType: 'blob' })
}

/* logs */
export function pageOperLogs(params) {
  return http.get('/admin/system/oper-log/page', { params })
}
export function getOperLog(id) {
  return http.get(`/admin/system/oper-log/${id}`)
}
export function pageLoginLogs(params) {
  return http.get('/admin/system/login-log/page', { params })
}

/* user oauth */
export function getUserOauth(userId) {
  return http.get(`/admin/system/user-oauth/user/${userId}`)
}
export function bindUserOauth(data) {
  return http.post('/admin/system/user-oauth', data)
}
export function unbindUserOauth(id) {
  return http.delete(`/admin/system/user-oauth/${id}`)
}
