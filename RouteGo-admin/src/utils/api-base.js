/**
 * 后端接口基地址的唯一来源，取自 .env.* 里的 VITE_API_BASE_URL。
 *
 * 不依赖同源反向代理：浏览器直接请求 env 中配置的后端地址，跨域由后端
 * `routego.security.allowed-origins` 白名单放行。
 */
export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/+$/, '')

/** 拼出完整接口地址；传入已是绝对地址时原样返回 */
export function apiUrl(path) {
  if (!path) return API_BASE_URL
  if (/^(https?:)?\/\//i.test(path) || path.startsWith('data:')) return path
  return `${API_BASE_URL}/${String(path).replace(/^\/+/, '')}`
}
