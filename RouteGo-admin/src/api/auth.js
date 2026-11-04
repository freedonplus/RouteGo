import { http } from './request'

export function getCaptcha() {
  return http.get('/admin/auth/captcha')
}

export function login(data) {
  return http.post('/admin/auth/login', data)
}

export function logout() {
  return http.post('/admin/auth/logout')
}

export function getMe() {
  return http.get('/admin/auth/me')
}

export function changePassword(data) {
  return http.put('/admin/auth/password', data)
}
