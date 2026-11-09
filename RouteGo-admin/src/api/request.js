import axios from 'axios'
import { mockAdapter } from '@/mock'
import { API_BASE_URL } from '@/utils/api-base'

export const TOKEN_KEY = 'rg-admin-token'

export class ApiError extends Error {
  constructor(code, message) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

export const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

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
  baseURL: API_BASE_URL,
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

/* ------------------------------------------------------------------ */
/* 进行中请求管理：路由切换时统一取消，旧页面的响应不再覆盖新页面       */
/* ------------------------------------------------------------------ */
const pending = new Map()
let pendingSeq = 0

/** 取消当前所有进行中的请求（router.afterEach 里调用） */
export function abortPendingRequests() {
  pending.forEach((controller) => controller.abort())
  pending.clear()
}

export function request(config) {
  const controller = new AbortController()
  const id = ++pendingSeq
  pending.set(id, controller)
  return http
    .request({ ...config, signal: controller.signal })
    .catch((error) => {
      // 主动取消的请求静默处理：既不进调用方的 catch（不弹错误提示），
      // 也不给调用方返回数据——页面已切换，结果无人消费
      if (axios.isCancel(error)) {
        return new Promise(() => {})
      }
      throw error
    })
    .finally(() => pending.delete(id))
}
