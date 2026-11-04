import axios from 'axios'
import { mockAdapter } from '@/mock'

export const TOKEN_KEY = 'rg-admin-token'

export class ApiError extends Error {
  constructor(code, message) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

export const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

// The production API authenticates with an HttpOnly cookie, so the bearer token
// is kept in memory only. Mock mode has no cookie, so its throwaway session
// token lives in sessionStorage (cleared when the tab closes).
let authToken = ''

export function setAuthToken(token) {
  authToken = token || ''
}

function currentToken() {
  if (authToken) return authToken
  return USE_MOCK ? sessionStorage.getItem(TOKEN_KEY) || '' : ''
}

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
})

if (USE_MOCK) {
  http.defaults.adapter = mockAdapter
}

function handleUnauthorized() {
  const hadSession = Boolean(authToken) || Boolean(sessionStorage.getItem(TOKEN_KEY))
  setAuthToken('')
  sessionStorage.removeItem(TOKEN_KEY)
  if (!hadSession || window.location.pathname === '/login') return
  const redirect = encodeURIComponent(window.location.pathname + window.location.search)
  window.location.assign(`/login?redirect=${redirect}`)
}

http.interceptors.request.use((config) => {
  const token = currentToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) return body.data
      if (body.code === 401) handleUnauthorized()
      throw new ApiError(body.code, body.msg || '请求失败')
    }
    return body
  },
  (error) => {
    if (error instanceof ApiError) throw error
    const body = error.response?.data
    const code = body?.code ?? error.response?.status ?? 0
    if (code === 401) handleUnauthorized()
    throw new ApiError(code, body?.msg || error.message || '网络错误')
  }
)

export function request(config) {
  return http.request(config)
}
