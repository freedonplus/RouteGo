import { http } from './request'

export function getCaptcha() {
  return http.get('/admin/auth/captcha')
}

/**
 * 账号密码登录。account 支持用户名 / 手机号 / 邮箱。
 */
export function login(data) {
  return http.post('/admin/auth/login', data)
}

/**
 * 发送短信 / 邮箱验证码。
 * @param {{ target: string, scene?: 'LOGIN'|'BIND'|'RESET_PASSWORD' }} data
 */
export function sendCode(data) {
  return http.post('/admin/auth/code', data)
}

/**
 * 验证码登录。
 */
export function loginByCode(data) {
  return http.post('/admin/auth/login/code', data)
}

/**
 * 已启用的第三方登录平台。
 */
export function getOauthProviders() {
  return http.get('/admin/auth/oauth/providers')
}

export function getOauthUrl(provider) {
  return http.get(`/admin/auth/oauth/${provider}/url`)
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
