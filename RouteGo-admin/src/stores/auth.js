import { defineStore } from 'pinia'
import {
  getCaptcha as getCaptchaApi,
  getMe,
  login as loginApi,
  logout as logoutApi,
} from '@/api/auth'
import { getUserMenuTree } from '@/api/system'
import { setAuthToken, TOKEN_KEY, USE_MOCK } from '@/api/request'

export function hasPermission(perms, needed) {
  if (!needed || (Array.isArray(needed) && needed.length === 0)) return true
  const list = Array.isArray(perms) ? perms : []
  if (list.includes('*')) return true
  if (Array.isArray(needed)) return needed.some((item) => list.includes(item))
  return list.includes(needed)
}

function initialToken() {
  return USE_MOCK ? sessionStorage.getItem(TOKEN_KEY) || '' : ''
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: initialToken(),
    user: null,
    menus: [],
    loaded: false,
    sessionVersion: 0,
  }),

  getters: {
    permissions: (state) => state.user?.permissions || [],
    hasPerm: (state) => (perm) => hasPermission(state.user?.permissions || [], perm),
    displayName: (state) => state.user?.nickname || state.user?.username || '未登录',
  },

  actions: {
    async fetchCaptcha() {
      return getCaptchaApi()
    },

    async login(payload) {
      const result = await loginApi(payload)
      this.token = result.tokenValue
      setAuthToken(result.tokenValue)
      if (USE_MOCK) sessionStorage.setItem(TOKEN_KEY, result.tokenValue)
      await this.loadSession()
      return result
    },

    async loadSession() {
      if (this.token) setAuthToken(this.token)
      const [me, menus] = await Promise.all([getMe(), getUserMenuTree()])
      this.user = me
      this.menus = menus || []
      this.loaded = true
      this.sessionVersion += 1
      return me
    },

    async logout() {
      try {
        await logoutApi()
      } catch {
        // 本地会话仍需清理
      } finally {
        this.clear()
      }
    },

    clear() {
      this.token = ''
      this.user = null
      this.menus = []
      this.loaded = false
      setAuthToken('')
      sessionStorage.removeItem(TOKEN_KEY)
    },
  },
})
