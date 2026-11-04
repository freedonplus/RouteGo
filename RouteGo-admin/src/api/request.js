import axios from 'axios'
import { mockAdapter } from '@/mock'

export const TOKEN_KEY = 'rg-admin-token'
export const USER_KEY = 'rg-admin-user'

export class ApiError extends Error {
  constructor(code, message) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

const useMock = import.meta.env.VITE_USE_MOCK !== 'false'

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
})

if (useMock) {
  http.defaults.adapter = mockAdapter
}

function handleUnauthorized() {
  const hadToken = Boolean(localStorage.getItem(TOKEN_KEY))
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  if (!hadToken || window.location.pathname === '/login') return
  const redirect = encodeURIComponent(window.location.pathname + window.location.search)
  window.location.assign(`/login?redirect=${redirect}`)
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
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
